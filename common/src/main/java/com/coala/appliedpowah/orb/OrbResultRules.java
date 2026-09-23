package com.coala.appliedpowah.orb;

/**
 * Pure OUTPUT-slot stacking rules (Inscriber-style). No Minecraft types — unit-testable.
 * Contract used by {@link EnergizingOrbBlockEntity#canPlaceResult} / {@code tryPlaceResult}.
 */
public final class OrbResultRules {

    public static final int MAX_SLOT = 64;

    private OrbResultRules() {
    }

    /**
     * @param currentCount  existing OUTPUT count (0 = empty)
     * @param sameItemSameTags whether existing stack matches the result
     * @param addCount      result count to place
     */
    public static boolean canPlace(int currentCount, boolean sameItemSameTags, int addCount) {
        if (addCount <= 0 || addCount > MAX_SLOT) {
            return false;
        }
        if (currentCount <= 0) {
            return true;
        }
        if (!sameItemSameTags) {
            return false;
        }
        return currentCount + addCount <= MAX_SLOT;
    }

    /** Resulting count after a successful place; -1 if refused. */
    public static int afterPlace(int currentCount, boolean sameItemSameTags, int addCount) {
        if (!canPlace(currentCount, sameItemSameTags, addCount)) {
            return -1;
        }
        return currentCount <= 0 ? addCount : currentCount + addCount;
    }

    /**
     * Settle gate: a return sink that takes the entire result is success.
     * Only the remainder needs OUTPUT room. (Calling canPlace on an empty
     * remainder wrongly refuses and can leak products if the sink was real.)
     */
    public static boolean canSettle(boolean returnSinkTakesAll, boolean outputCanPlaceRemainder) {
        return returnSinkTakesAll || outputCanPlaceRemainder;
    }
}
