package com.coala.appliedpowah.orb;

import com.coala.appliedpowah.AppliedPowah;
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
 * Shared energizing recipe helpers for one {@link CraftThread} page.
 * Slot layout per page: 0..5 = inputs, 6 = output (see {@link CraftThread}).
 */
public final class EnergizingOrbLogic {
    public static final int SLOTS = CraftThread.SIZE;
    public static final int OUTPUT = CraftThread.OUTPUT;
    public static final int INPUTS = CraftThread.INPUTS;

    private EnergizingOrbLogic() {
    }

    /**
     * Input slot: empty slot + non-rod. Output slot is independent (AE2 machine
     * semantics) — having a result in the output slot does not block new inputs.
     * Rods never enter craft input slots.
     */
    public static boolean canInsertInput(ItemStackHandler inv, int index, ItemStack stack) {
        if (index == OUTPUT || index >= inv.getSlots() || index < 0 || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof com.coala.appliedpowah.chargingrod.RodBlockItem) {
            return false;
        }
        return inv.getStackInSlot(index).isEmpty();
    }

    @Nullable
    public static EnergizingRecipe findRecipe(Level level, ItemStackHandler pageInv) {
        if (level == null || pageInv == null) {
            return null;
        }
        try {
            // Count filled input slots first — Powah requires exact ingredient cardinality.
            int filled = 0;
            for (int i = 0; i < INPUTS && i < pageInv.getSlots(); i++) {
                if (!pageInv.getStackInSlot(i).isEmpty()) {
                    filled++;
                }
            }
            if (filled == 0) {
                return null;
            }
            owmii.powah.lib.logistics.inventory.ItemStackHandler powahInv =
                    new owmii.powah.lib.logistics.inventory.ItemStackHandler(SLOTS);
            for (int i = 0; i < SLOTS && i < pageInv.getSlots(); i++) {
                if (i == OUTPUT) {
                    powahInv.setStackInSlot(i, ItemStack.EMPTY);
                } else {
                    ItemStack src = pageInv.getStackInSlot(i);
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
            AppliedPowah.LOG.warn("findRecipe failed: {}", t.toString());
            return null;
        }
    }

    public static ItemStack resultOf(Level level, EnergizingRecipe recipe) {
        try {
            return recipe.getResultItem(level.registryAccess()).copy();
        } catch (Exception e) {
            try {
                return recipe.getResultItem().copy();
            } catch (Exception e2) {
                AppliedPowah.LOG.error("recipe result unreadable: {}", e2.toString());
                return ItemStack.EMPTY;
            }
        }
    }

    public static void clearInputs(ItemStackHandler pageInv) {
        for (int i = 0; i < INPUTS && i < pageInv.getSlots(); i++) {
            pageInv.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static boolean matchesAnyType(Recipe<?> r) {
        return r != null && Recipes.ENERGIZING.get() == r.getType();
    }
}
