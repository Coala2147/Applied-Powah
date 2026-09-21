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
     * Input slots accept items when the slot itself is empty.
     * Pending product in output must NOT block refill (unlike raw Powah).
     * Recipe matching ignores slot 0.
     * Do NOT change product stack size / multi-complete semantics here.
     */
    public static boolean canInsertInput(ItemStackHandler inv, int index, ItemStack stack) {
        return index != OUTPUT
                && index < inv.getSlots()
                && inv.getStackInSlot(index).isEmpty()
                && !stack.isEmpty();
    }

    @Nullable
    public static EnergizingRecipe findRecipe(Level level, ItemStackHandler inv) {
        if (level == null) {
            return null;
        }
        try {
            owmii.powah.lib.logistics.inventory.ItemStackHandler powahInv =
                    new owmii.powah.lib.logistics.inventory.ItemStackHandler(SLOTS);
            for (int i = 0; i < SLOTS && i < inv.getSlots(); i++) {
                if (i == OUTPUT) {
                    powahInv.setStackInSlot(i, ItemStack.EMPTY);
                } else {
                    powahInv.setStackInSlot(i, inv.getStackInSlot(i));
                }
            }
            Optional<EnergizingRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(Recipes.ENERGIZING.get(), new RecipeWrapper(powahInv), level);
            return recipe.orElse(null);
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
