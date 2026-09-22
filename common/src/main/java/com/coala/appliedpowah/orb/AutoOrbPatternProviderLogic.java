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
        // Busy only when the in-flight sendList still has leftovers (our push
        // consumes inputs directly into the orb, so sendList stays empty).
        if (isBusy()) {
            return false;
        }
        // Orb already has a recipe mid-run — wait until output is taken.
        if (autoOrb.containRecipe()) {
            return false;
        }
        boolean ok = autoOrb.acceptPatternInputs(inputHolder);
        if (ok) {
            // Mirror PatternProviderLogic.onPushPatternSuccess for LOCK_UNTIL_* modes.
            // getLogic().exportSettings is not needed; lock state is owned by this logic.
        }
        return ok;
    }
}
