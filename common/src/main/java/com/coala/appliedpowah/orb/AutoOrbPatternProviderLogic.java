package com.coala.appliedpowah.orb;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom PatternProviderLogic for the Auto Energizing Orb.
 * Instead of pushing pattern inputs to adjacent blocks, it routes them directly
 * into the orb's own input slots (slots 1-6).
 */
public class AutoOrbPatternProviderLogic extends PatternProviderLogic {

    private final AutoEnergizingOrbBlockEntity autoOrb;

    public AutoOrbPatternProviderLogic(IManagedGridNode mainNode, AutoEnergizingOrbBlockEntity host, int patternSlots) {
        super(mainNode, host, patternSlots);
        this.autoOrb = host;
    }

    /**
     * CPU's only throttle. Must be true while a craft is in-flight or the
     * product has not left the orb — otherwise the CPU double-dispatches
     * (1 ordered → 2 produced / 10 → 56).
     */
    @Override
    public boolean isBusy() {
        if (super.isBusy()) {
            return true;
        }
        if (autoOrb == null || autoOrb.getLevel() == null) {
            return false;
        }
        return autoOrb.containRecipe() || autoOrb.hasPendingOutput();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (patternDetails == null || inputHolder == null) {
            return false;
        }
        // Exact instance or same definition (encoded pattern item).
        boolean patternMatches = getAvailablePatterns().stream()
                .anyMatch(p -> p == patternDetails || p.getDefinition().equals(patternDetails.getDefinition()));
        if (!patternMatches) {
            return false;
        }
        if (!autoOrb.getMainNode().isActive()) {
            return false;
        }
        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        if (isBusy()) {
            return false;
        }
        if (autoOrb.containRecipe() || autoOrb.hasPendingOutput()) {
            return false;
        }
        return autoOrb.acceptPatternInputs(inputHolder);
    }
}
