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
        // Match pattern by definition (encoded pattern item)
        boolean patternMatches = getAvailablePatterns().stream()
                .anyMatch(p -> p.getDefinition().equals(patternDetails.getDefinition()));
        if (!patternMatches) {
            return false;
        }

        // Network must be active
        if (!autoOrb.getMainNode().isActive()) {
            return false;
        }

        // Crafting lock must be open
        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }

        // Not busy (sendList / return inv empty)
        if (isBusy()) {
            return false;
        }

        // Push inputs into the orb's own input slots
        return autoOrb.acceptPatternInputs(inputHolder);
    }
}
