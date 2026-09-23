package com.coala.appliedpowah.orb;

/**
 * Pure acceptsPlans / full-set gating for ICraftingMachine. No Minecraft types.
 */
public final class OrbCraftGate {

    private OrbCraftGate() {
    }

    /**
     * @param completing     mid completeIfPossible
     * @param containRecipe  inputs already match a recipe (a full set waiting for energy)
     * @param emptyInputSlots number of empty craft input slots (1..6)
     */
    public static boolean acceptsPlans(boolean completing, boolean containRecipe, int emptyInputSlots) {
        if (completing || containRecipe) {
            return false;
        }
        return emptyInputSlots > 0;
    }

    /** All-or-nothing: need at least {@code requiredEmpty} free slots. */
    public static boolean canAcceptFullSet(int emptyInputSlots, int requiredEmpty) {
        return requiredEmpty > 0 && emptyInputSlots >= requiredEmpty;
    }

    /**
     * AE2 PatternProvider blocking: when enabled, refuse push if the target
     * already contains any key from the provider's pattern-input set.
     */
    public static boolean blocksPush(boolean isBlocking, boolean targetContainsPatternInput) {
        return isBlocking && targetContainsPatternInput;
    }
}
