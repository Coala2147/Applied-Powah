package com.coala.appliedpowah.meorb;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * ME Energizing Orb input router.
 * Architecture from Mek-Energistics MePatternInputRouter (plan then commit, full rollback),
 * renamed into this project (com.coala.appliedpowah.meorb).
 */
public final class MeInputRouter {

    private static final int MAX_ASSIGNMENT_STEPS = 256;

    private MeInputRouter() {
    }

    /** One-item craft input port (orb slots 1-6). */
    public interface InputPort {
        boolean empty();

        boolean supports(AEItemKey key);

        long insert(AEItemKey key, long amount, boolean simulate);

        void restore();
    }

    public static boolean route(KeyCounter[] inputHolders, List<? extends InputPort> ports) {
        List<AEItemKey> singles = expandSingles(inputHolders);
        if (singles == null || singles.isEmpty() || ports == null || ports.isEmpty()) {
            return false;
        }
        if (singles.size() > ports.size()) {
            return false;
        }

        List<InputPort> touched = new ArrayList<>();
        int steps = 0;
        int idx = 0;
        try {
            for (InputPort port : ports) {
                if (idx >= singles.size()) {
                    break;
                }
                if (++steps > MAX_ASSIGNMENT_STEPS) {
                    restoreAll(touched);
                    return false;
                }
                AEItemKey key = singles.get(idx);
                if (!port.empty() || !port.supports(key)) {
                    continue;
                }
                long accepted = port.insert(key, 1, false);
                if (accepted != 1) {
                    restoreAll(touched);
                    return false;
                }
                touched.add(port);
                idx++;
            }
        } catch (RuntimeException ex) {
            restoreAll(touched);
            return false;
        }
        return idx == singles.size();
    }

    private static void restoreAll(List<InputPort> touched) {
        for (InputPort p : touched) {
            try {
                p.restore();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static List<AEItemKey> expandSingles(KeyCounter[] inputHolders) {
        if (inputHolders == null) {
            return null;
        }
        List<AEItemKey> singles = new ArrayList<>();
        for (KeyCounter counter : inputHolders) {
            if (counter == null || counter.isEmpty()) {
                continue;
            }
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return null;
                }
                for (long n = 0; n < entry.getLongValue(); n++) {
                    singles.add(itemKey);
                }
            }
        }
        return singles;
    }
}
