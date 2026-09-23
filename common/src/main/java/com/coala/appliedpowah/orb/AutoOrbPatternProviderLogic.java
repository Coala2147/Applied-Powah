package com.coala.appliedpowah.orb;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;

/**
 * PatternProviderLogic for the ME Energizing Orb.
 * CPU dispatch + truthful isBusy throttle. Input intake is transactional
 * (see {@link AutoEnergizingOrbBlockEntity#acceptPatternInputs}).
 * Passive self-crafting lives on the BE (Mek MePassiveCraftingDispatcher).
 */
public class AutoOrbPatternProviderLogic extends PatternProviderLogic {

    private final AutoEnergizingOrbBlockEntity orb;

    public AutoOrbPatternProviderLogic(IManagedGridNode mainNode, AutoEnergizingOrbBlockEntity host, int patternSlots) {
        super(mainNode, host, patternSlots);
        this.orb = host;
    }

    @Override
    public boolean isBusy() {
        if (super.isBusy()) {
            return true;
        }
        if (orb == null || orb.getLevel() == null) {
            return false;
        }
        return orb.containRecipe() || orb.hasPendingOutput();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (patternDetails == null || inputHolder == null) {
            return false;
        }
        if (!isOurPattern(patternDetails)) {
            return false;
        }
        if (orb.getMainNode() == null || !orb.getMainNode().isActive()) {
            return false;
        }
        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        if (isBusy() || orb.containRecipe() || orb.hasPendingOutput()) {
            return false;
        }
        return orb.acceptPatternInputs(inputHolder);
    }

    private boolean isOurPattern(IPatternDetails patternDetails) {
        for (IPatternDetails p : getAvailablePatterns()) {
            if (p == patternDetails) {
                return true;
            }
            if (p.getDefinition() != null && p.getDefinition().equals(patternDetails.getDefinition())) {
                return true;
            }
        }
        return false;
    }
}
