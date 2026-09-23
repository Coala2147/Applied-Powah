package com.coala.appliedpowah.meorb;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;

/**
 * ME 充能台样板仓 + CPU 握手（EAE 1.21.1 样板页）。
 */
public class MePatternLogic extends PatternProviderLogic {

    private final MeEnergizingOrbBlockEntity meOrb;

    public MePatternLogic(IManagedGridNode mainNode, MeEnergizingOrbBlockEntity host, int patternSlots) {
        super(mainNode, host, patternSlots);
        this.meOrb = host;
    }

    @Override
    public boolean isBusy() {
        if (super.isBusy()) {
            return true;
        }
        if (meOrb == null || meOrb.getLevel() == null) {
            return false;
        }
        return meOrb.isCrafting() || meOrb.hasPendingProduct();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (patternDetails == null || inputHolder == null) {
            return false;
        }
        if (!holdsPattern(patternDetails)) {
            return false;
        }
        if (meOrb.getMainNode() == null || !meOrb.getMainNode().isActive()) {
            return false;
        }
        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        if (isBusy()) {
            return false;
        }
        return meOrb.acceptCpuPatternInputs(inputHolder);
    }

    private boolean holdsPattern(IPatternDetails patternDetails) {
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
