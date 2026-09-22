package com.coala.appliedpowah.orb;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import owmii.powah.block.energizing.EnergizingRecipe;
import owmii.powah.lib.logistics.inventory.RecipeWrapper;
import owmii.powah.recipe.Recipes;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Shared energizing recipe helpers. Slot layout matches Powah orb:
 * slot 0 = output, slots 1..6 = inputs.
 */
public final class EnergizingOrbLogic {
    public static final int SLOTS = 7;
    public static final int OUTPUT = 0;

    private EnergizingOrbLogic() {
    }

    /**
     * Input slot: empty slot + non-rod. Output slot is independent (AE2 machine
     * semantics) — having a result in slot 0 does not block new inputs.
     * Rods never enter craft input slots.
     */
    public static boolean canInsertInput(ItemStackHandler inv, int index, ItemStack stack) {
        if (index == OUTPUT || index >= inv.getSlots() || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof com.coala.appliedpowah.chargingrod.RodBlockItem) {
            return false;
        }
        return inv.getStackInSlot(index).isEmpty();
    }

    @Nullable
    public static EnergizingRecipe findRecipe(Level level, ItemStackHandler inv) {
        if (level == null) {
            return null;
        }
        try {
            // Count filled input slots first — Powah requires exact ingredient cardinality.
            int filled = 0;
            for (int i = 1; i < SLOTS && i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    filled++;
                }
            }
            if (filled == 0) {
                return null;
            }
            owmii.powah.lib.logistics.inventory.ItemStackHandler powahInv =
                    new owmii.powah.lib.logistics.inventory.ItemStackHandler(SLOTS);
            for (int i = 0; i < SLOTS && i < inv.getSlots(); i++) {
                if (i == OUTPUT) {
                    powahInv.setStackInSlot(i, ItemStack.EMPTY);
                } else {
                    ItemStack src = inv.getStackInSlot(i);
                    powahInv.setStackInSlot(i, src.isEmpty() ? ItemStack.EMPTY : src.copy());
                }
            }
            Optional<EnergizingRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(Recipes.ENERGIZING.get(), new RecipeWrapper(powahInv), level);
            EnergizingRecipe found = recipe.orElse(null);
            if (found != null && found.getIngredients().size() != filled) {
                // Extra or missing input slots — refuse (6×diamond must not match 1-ingredient).
                return null;
            }
            return found;
        } catch (Throwable t) {
            return null;
        }
    }

    public static ItemStack resultOf(Level level, EnergizingRecipe recipe) {
        try {
            return recipe.getResultItem(level.registryAccess()).copy();
        } catch (Throwable t) {
            try {
                return recipe.getResultItem().copy();
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
    }

    public static void clearInputs(ItemStackHandler inv) {
        for (int i = 1; i < SLOTS && i < inv.getSlots(); i++) {
            inv.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static boolean matchesAnyType(Recipe<?> r) {
        return r != null && Recipes.ENERGIZING.get() == r.getType();
    }
}
