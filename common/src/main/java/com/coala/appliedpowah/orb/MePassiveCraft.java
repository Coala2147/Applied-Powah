package com.coala.appliedpowah.orb;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Passive / self-crafting from patterns held by this machine.
 * Ported from Mek-Energistics MePassiveCraftingDispatcher + MePassiveCraftingSettings
 * (refs/1.21.1/Mek-Energistics-main) — 1.20.1 / AE2 v15 adaptation.
 *
 * Flow: scan own patterns → plan inputs from ME storage → extract → submit to
 * machine (transactional accept). On submit failure the extract is restored.
 */
public final class MePassiveCraft {

    public static final int DEFAULT_INTERVAL_TICKS = 20;
    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int MAX_INTERVAL_TICKS = 72_000;
    public static final long MAX_COPIES_PER_RUN = 64;
    public static final int MAX_PATTERN_SCAN_PER_RUN = 8;

    private int intervalTicks = DEFAULT_INTERVAL_TICKS;
    private long multiplier = 1;
    private int countdown = DEFAULT_INTERVAL_TICKS;
    private int patternScanCursor;
    private final List<RecoveryEntry> recoveryBuffer = new ArrayList<>();

    private MePassiveCraft() {
    }

    public static MePassiveCraft create() {
        return new MePassiveCraft();
    }

    public long multiplier() {
        return multiplier;
    }

    public void set(int requestedInterval, long requestedMultiplier) {
        intervalTicks = Math.max(MIN_INTERVAL_TICKS, Math.min(requestedInterval, MAX_INTERVAL_TICKS));
        multiplier = Math.max(1, Math.min(requestedMultiplier, MAX_COPIES_PER_RUN));
        countdown = intervalTicks;
    }

    /** True when the next passive run should fire. */
    public boolean tick() {
        if (--countdown > 0) {
            return false;
        }
        countdown = intervalTicks;
        return true;
    }

    public void save(CompoundTag tag) {
        tag.putInt("PassiveCraftInterval", intervalTicks);
        tag.putLong("PassiveCraftMultiplier", multiplier);
        tag.putInt("PassiveCraftScanCursor", patternScanCursor);
        if (recoveryBuffer.isEmpty()) {
            tag.remove("PassiveCraftRecovery");
            return;
        }
        ListTag list = new ListTag();
        for (RecoveryEntry e : recoveryBuffer) {
            CompoundTag t = new CompoundTag();
            t.put("Key", e.key().toTagGeneric());
            t.putLong("Amount", e.amount());
            list.add(t);
        }
        tag.put("PassiveCraftRecovery", list);
    }

    public void load(CompoundTag tag) {
        set(tag.contains("PassiveCraftInterval") ? tag.getInt("PassiveCraftInterval") : DEFAULT_INTERVAL_TICKS,
                tag.contains("PassiveCraftMultiplier") ? tag.getLong("PassiveCraftMultiplier") : 1);
        patternScanCursor = tag.contains("PassiveCraftScanCursor") ? tag.getInt("PassiveCraftScanCursor") : 0;
        recoveryBuffer.clear();
        ListTag list = tag.getList("PassiveCraftRecovery", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                AEKey key = AEKey.fromTagGeneric(list.getCompound(i).getCompound("Key"));
                long amount = list.getCompound(i).getLong("Amount");
                if (key != null && amount > 0) {
                    recoveryBuffer.add(new RecoveryEntry(key, amount));
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void bufferRemainder(AEKey key, long amount) {
        if (key == null || amount <= 0) {
            return;
        }
        for (int i = 0; i < recoveryBuffer.size(); i++) {
            RecoveryEntry e = recoveryBuffer.get(i);
            if (e.key().equals(key)) {
                long merged = e.amount() > Long.MAX_VALUE - amount ? Long.MAX_VALUE : e.amount() + amount;
                recoveryBuffer.set(i, new RecoveryEntry(key, merged));
                return;
            }
        }
        recoveryBuffer.add(new RecoveryEntry(key, amount));
    }

    public List<RecoveryEntry> drainRecoveryBuffer() {
        List<RecoveryEntry> drained = List.copyOf(recoveryBuffer);
        recoveryBuffer.clear();
        return drained;
    }

    /**
     * Mek MePassiveCraftingDispatcher.submitAvailable.
     * Scan patterns, pull ingredients from ME storage, hand them to {@code submitter}.
     */
    public boolean submitAvailable(List<IPatternDetails> patterns, Level level,
            MEStorage storage, IActionSource source, Predicate<KeyCounter[]> submitter) {
        boolean submitted = flushRecoveryBuffer(storage, source);
        int size = patterns.size();
        if (size == 0) {
            return submitted;
        }
        int cursor = patternScanCursor;
        if (cursor < 0 || cursor >= size) {
            cursor = 0;
        }
        long requested = multiplier;
        int cursorSteps = 0;
        for (int scanned = 0; scanned < MAX_PATTERN_SCAN_PER_RUN && cursorSteps < size; scanned++) {
            cursorSteps++;
            IPatternDetails pattern = patterns.get(cursor);
            KeyCounter[] inputs = plan(pattern, requested, level, storage, source);
            if (inputs == null) {
                cursor = advance(cursor, size);
                continue;
            }
            KeyCounter extracted = extract(inputs, storage, source);
            if (extracted == null) {
                cursor = advance(cursor, size);
                continue;
            }
            if (submitter.test(inputs)) {
                submitted = true;
                cursor = advance(cursor, size);
                continue;
            }
            restoreRemainder(extracted, storage, source);
            cursor = advance(cursor, size);
        }
        patternScanCursor = cursor;
        return submitted;
    }

    private static int advance(int cursor, int size) {
        return size <= 0 ? 0 : (cursor + 1) % size;
    }

    private void restoreRemainder(KeyCounter extracted, MEStorage storage, IActionSource source) {
        for (var entry : extracted) {
            long amount = entry.getLongValue();
            if (amount <= 0) {
                continue;
            }
            long inserted = storage.insert(entry.getKey(), amount, Actionable.MODULATE, source);
            long remainder = amount - inserted;
            if (remainder > 0) {
                bufferRemainder(entry.getKey(), remainder);
            }
        }
    }

    private boolean flushRecoveryBuffer(MEStorage storage, IActionSource source) {
        boolean changed = false;
        for (RecoveryEntry entry : drainRecoveryBuffer()) {
            long amount = entry.amount();
            if (amount <= 0) {
                continue;
            }
            long inserted = storage.insert(entry.key(), amount, Actionable.MODULATE, source);
            if (inserted >= amount) {
                changed = true;
            } else {
                bufferRemainder(entry.key(), amount - inserted);
            }
        }
        return changed;
    }

    private static KeyCounter[] plan(IPatternDetails pattern, long copies, Level level,
            MEStorage storage, IActionSource source) {
        if (copies < 1) {
            return null;
        }
        Map<AEKey, Long> reserved = new HashMap<>();
        IPatternDetails.IInput[] patternInputs = pattern.getInputs();
        KeyCounter[] result = new KeyCounter[patternInputs.length];
        for (int index = 0; index < patternInputs.length; index++) {
            result[index] = new KeyCounter();
            IPatternDetails.IInput input = patternInputs[index];
            if (input == null) {
                continue;
            }
            long required;
            try {
                required = Math.multiplyExact(input.getMultiplier(), copies);
            } catch (ArithmeticException ignored) {
                return null;
            }
            for (GenericStack possible : input.getPossibleInputs()) {
                if (possible == null || possible.what() == null || !input.isValid(possible.what(), level)) {
                    continue;
                }
                AEKey key = possible.what();
                long available = storage.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, source);
                long amount = Math.min(required, Math.max(0, available - reserved.getOrDefault(key, 0L)));
                if (amount > 0) {
                    result[index].add(key, amount);
                    reserved.merge(key, amount, Long::sum);
                    required -= amount;
                }
                if (required == 0) {
                    break;
                }
            }
            if (required != 0) {
                return null;
            }
        }
        return result;
    }

    private KeyCounter extract(KeyCounter[] inputs, MEStorage storage, IActionSource source) {
        KeyCounter extracted = new KeyCounter();
        for (KeyCounter input : inputs) {
            for (var entry : input) {
                long amount = storage.extract(entry.getKey(), entry.getLongValue(), Actionable.MODULATE, source);
                if (amount > 0) {
                    extracted.add(entry.getKey(), amount);
                }
                if (amount != entry.getLongValue()) {
                    restoreRemainder(extracted, storage, source);
                    return null;
                }
            }
        }
        return extracted;
    }

    private record RecoveryEntry(AEKey key, long amount) {
    }
}
