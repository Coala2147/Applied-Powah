package com.coala.appliedpowah.orb;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;

import java.util.ArrayList;
import java.util.List;

/**
 * PatternProviderLogic for the ME Energizing Orb.
 *
 * CPU talks to this object; we never forward pattern inputs to neighbours.
 * Instead they are transactionally routed into the orb's own input slots
 * (Mek-Energistics factory discipline — see
 * docs/feat/调研_ME充能台_熔炼工厂架构与ExtendedAE样板页.md).
 */
public class AutoOrbPatternProviderLogic extends PatternProviderLogic {

    private final AutoEnergizingOrbBlockEntity orb;

    public AutoOrbPatternProviderLogic(IManagedGridNode mainNode, AutoEnergizingOrbBlockEntity host, int patternSlots) {
        super(mainNode, host, patternSlots);
        this.orb = host;
    }

    /**
     * CPU's only throttle (Mek discipline 2). Must stay true while a craft is
     * in-flight or the product has not left the machine — otherwise the CPU
     * double-dispatches (1 ordered → 2 produced).
     */
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
        // One job at a time.
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

    /** Flatten AE2 KeyCounter[] into single-item stacks (2×diamond → two stacks). */
    static List<ItemStackSingle> flatten(KeyCounter[] inputHolder) {
        List<ItemStackSingle> singles = new ArrayList<>();
        if (inputHolder == null) {
            return singles;
        }
        for (KeyCounter counter : inputHolder) {
            if (counter == null || counter.isEmpty()) {
                continue;
            }
            for (var entry : counter) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (!(key instanceof AEItemKey itemKey) || amount <= 0) {
                    return null;
                }
                for (long n = 0; n < amount; n++) {
                    singles.add(new ItemStackSingle(itemKey));
                }
            }
        }
        return singles;
    }

    record ItemStackSingle(AEItemKey key) {
    }
}
