package com.coala.appliedpowah.meorb;

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
 * ME Energizing Orb self-craft engine.
 * Architecture from Mek-Energistics MePassiveCraftingDispatcher + MePassiveCraftingSettings,
 * renamed into this project (com.coala.appliedpowah.meorb).
 */
public final class MeSelfCraftSettings {

    public static final int DEFAULT_INTERVAL_TICKS = 20;
    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int MAX_INTERVAL_TICKS = 72_000;
    public static final long MAX_COPIES_PER_RUN = 64;
    public static final int MAX_PATTERN_SCAN_PER_RUN = 8;
    private static final String TAG_BUFFER = "MeSelfCraftRecovery";

    private int intervalTicks = DEFAULT_INTERVAL_TICKS;
    private long multiplier = 1;
    private int countdown = DEFAULT_INTERVAL_TICKS;
    private int patternScanCursor;
    private final List<RecoveryEntry> recoveryBuffer = new ArrayList<>();
    private int patternsScanned;
    private int patternsRejected;
    private int patternsRestored;
    private int patternsSubmitted;
    private int bufferFlushes;

    public int intervalTicks() {
        return intervalTicks;
    }

    public long multiplier() {
        return multiplier;
    }

    public int patternScanCursor() {
        return patternScanCursor;
    }

    public int patternScanBudget() {
        return MAX_PATTERN_SCAN_PER_RUN;
    }

    public long cappedCopies(long requestedCopies) {
        return Math.max(1, Math.min(requestedCopies, MAX_COPIES_PER_RUN));
    }

    public void set(int requestedIntervalTicks, long requestedMultiplier) {
        intervalTicks = Math.max(MIN_INTERVAL_TICKS, Math.min(requestedIntervalTicks, MAX_INTERVAL_TICKS));
        multiplier = cappedCopies(requestedMultiplier);
        countdown = intervalTicks;
    }

    public boolean tick() {
        if (--countdown > 0) {
            return false;
        }
        countdown = intervalTicks;
        return true;
    }

    public void save(CompoundTag tag) {
        tag.putInt("MeSelfCraftInterval", intervalTicks);
        tag.putLong("MeSelfCraftMultiplier", multiplier);
        tag.putInt("MeSelfCraftScanCursor", patternScanCursor);
        if (recoveryBuffer.isEmpty()) {
            tag.remove(TAG_BUFFER);
            return;
        }
        ListTag buffer = new ListTag();
        for (RecoveryEntry entry : recoveryBuffer) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("Key", entry.key().toTagGeneric());
            entryTag.putLong("Amount", entry.amount());
            buffer.add(entryTag);
        }
        tag.put(TAG_BUFFER, buffer);
    }

    public void load(CompoundTag tag) {
        set(tag.contains("MeSelfCraftInterval") ? tag.getInt("MeSelfCraftInterval") : DEFAULT_INTERVAL_TICKS,
                tag.contains("MeSelfCraftMultiplier") ? tag.getLong("MeSelfCraftMultiplier") : 1L);
        patternScanCursor = tag.contains("MeSelfCraftScanCursor") ? tag.getInt("MeSelfCraftScanCursor") : 0;
        recoveryBuffer.clear();
        ListTag buffer = tag.getList(TAG_BUFFER, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < buffer.size(); i++) {
            try {
                AEKey key = AEKey.fromTagGeneric(buffer.getCompound(i).getCompound("Key"));
                long amount = buffer.getCompound(i).getLong("Amount");
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
            RecoveryEntry entry = recoveryBuffer.get(i);
            if (entry.key.equals(key)) {
                long merged = entry.amount > Long.MAX_VALUE - amount ? Long.MAX_VALUE : entry.amount + amount;
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

    public void beginRun() {
        patternsScanned = 0;
        patternsRejected = 0;
        patternsRestored = 0;
        patternsSubmitted = 0;
        bufferFlushes = 0;
    }

    public void recordPatternScanned() {
        patternsScanned++;
    }

    public void recordRejected() {
        patternsRejected++;
    }

    public void recordRestored() {
        patternsRestored++;
    }

    public void recordSubmitted() {
        patternsSubmitted++;
    }

    public void recordBufferFlush() {
        bufferFlushes++;
    }

    public void setPatternScanCursor(int cursor) {
        patternScanCursor = Math.max(0, cursor);
    }

    public record RecoveryEntry(AEKey key, long amount) {
        public RecoveryEntry {
            if (amount <= 0) {
                throw new IllegalArgumentException("Recovery amounts must be positive");
            }
        }
    }

    /**
     * Scan held patterns, pull ingredients from ME storage, hand them to the machine.
     * Failure restores the whole extract (Mek dispatcher discipline).
     */
    public boolean submitAvailable(List<IPatternDetails> patterns, long copies, Level level,
            MEStorage storage, IActionSource source, Predicate<KeyCounter[]> submitter) {
        beginRun();
        boolean submitted = flushRecoveryBuffer(storage, source);
        int size = patterns == null ? 0 : patterns.size();
        if (size == 0) {
            return submitted;
        }
        int cursor = patternScanCursor;
        if (cursor < 0 || cursor >= size) {
            cursor = 0;
        }
        int budget = patternScanBudget();
        long requested = cappedCopies(copies);
        int cursorSteps = 0;
        for (int scanned = 0; scanned < budget && cursorSteps < size; scanned++) {
            cursorSteps++;
            IPatternDetails pattern = patterns.get(cursor);
            recordPatternScanned();
            KeyCounter[] inputs = plan(pattern, requested, level, storage, source);
            if (inputs == null) {
                recordRejected();
                cursor = advance(cursor, size);
                continue;
            }
            KeyCounter extracted = extract(inputs, storage, source);
            if (extracted == null) {
                recordRestored();
                cursor = advance(cursor, size);
                continue;
            }
            if (submitter.test(inputs)) {
                submitted = true;
                recordSubmitted();
                cursor = advance(cursor, size);
                continue;
            }
            restoreRemainder(extracted, storage, source);
            recordRestored();
            cursor = advance(cursor, size);
        }
        setPatternScanCursor(cursor);
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
                recordBufferFlush();
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
}
