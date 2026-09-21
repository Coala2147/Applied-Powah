package com.coala.appliedpowah.jei;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.energycell.APCells;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import owmii.powah.block.energizing.EnergizingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI plugin.
 * <ul>
 *   <li>Registers Applied Powah rods as catalysts on Powah's Energizing category
 *       (same uid {@code powah:energizing}) — rods supply the orb, they are not
 *       crafting ingredients of energizing recipes.</li>
 *   <li>Ingredient info pages for cells and rods.</li>
 * </ul>
 * Crafting recipes remain vanilla data and are listed automatically.
 * Class is only loaded when JEI is present ({@code @JeiPlugin}).
 */
@JeiPlugin
public class APJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(AppliedPowah.MOD_ID, "jei_plugin");

    /** Identity must match Powah EnergizingCategory.TYPE (powah:energizing). */
    private static final RecipeType<EnergizingRecipe> POWAH_ENERGIZING =
            RecipeType.create("powah", "energizing", EnergizingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        try {
            APCells.AE_RODS.forEach((tier, block) ->
                    registration.addRecipeCatalyst(new ItemStack(block.get()), POWAH_ENERGIZING));
            APCells.ME_RODS.forEach((tier, block) ->
                    registration.addRecipeCatalyst(new ItemStack(block.get()), POWAH_ENERGIZING));
            AppliedPowah.LOG.info("JEI: registered AP rods as powah:energizing catalysts");
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("JEI catalyst registration skipped: {}", t.toString());
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        try {
            List<ItemStack> stacks = new ArrayList<>();
            stacks.add(new ItemStack(APCells.SUPER_DENSE_ITEM.get()));
            stacks.add(new ItemStack(APCells.EXTREME_DENSE_ITEM.get()));
            for (var e : APCells.AE_RODS.values()) {
                stacks.add(new ItemStack(e.get()));
            }
            for (var e : APCells.ME_RODS.values()) {
                stacks.add(new ItemStack(e.get()));
            }
            for (ItemStack stack : stacks) {
                List<Component> info = new ArrayList<>();
                info.add(Component.translatable("jei.applied_powah.info_header"));
                if (stack.getItem() instanceof com.coala.appliedpowah.chargingrod.RodBlockItem) {
                    info.add(Component.translatable("jei.applied_powah.rod_craft_note"));
                    info.add(Component.translatable("jei.applied_powah.rod_place_note"));
                    info.add(Component.translatable("jei.applied_powah.rod_feed_note"));
                } else {
                    info.add(Component.translatable("jei.applied_powah.cell_note"));
                }
                registration.addIngredientInfo(stack, VanillaTypes.ITEM_STACK, info.toArray(new Component[0]));
            }
            AppliedPowah.LOG.info("JEI: registered {} Applied Powah info entries", stacks.size());
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("JEI info registration skipped: {}", t.toString());
        }
    }
}
