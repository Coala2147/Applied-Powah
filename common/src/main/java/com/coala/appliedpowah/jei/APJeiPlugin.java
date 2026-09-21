package com.coala.appliedpowah.jei;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.energycell.APCells;
import com.coala.appliedpowah.energycell.ExtremeDenseEnergyCellBlock;
import com.coala.appliedpowah.energycell.SuperDenseEnergyCellBlock;
import com.coala.appliedpowah.orb.APOrbs;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import owmii.powah.block.energizing.EnergizingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI: AP rods + orbs as catalysts on Powah energizing;
 * energy cells: empty only in JEI index (charged creative variants hidden).
 */
@JeiPlugin
public class APJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(AppliedPowah.MOD_ID, "jei_plugin");

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
            if (APOrbs.ME_ORB_ITEM != null && APOrbs.ME_ORB_ITEM.isPresent()) {
                registration.addRecipeCatalyst(new ItemStack(APOrbs.ME_ORB_ITEM.get()), POWAH_ENERGIZING);
            }
            if (APOrbs.ADV_ORB_ITEM != null && APOrbs.ADV_ORB_ITEM.isPresent()) {
                registration.addRecipeCatalyst(new ItemStack(APOrbs.ADV_ORB_ITEM.get()), POWAH_ENERGIZING);
            }
            AppliedPowah.LOG.info("JEI: AP rods+orbs registered as powah:energizing catalysts");
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("JEI catalyst registration skipped: {}", t.toString());
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        try {
            List<ItemStack> empty = List.of(
                    new ItemStack(APCells.SUPER_DENSE_ITEM.get()),
                    new ItemStack(APCells.EXTREME_DENSE_ITEM.get()));
            List<ItemStack> charged = new ArrayList<>();
            charged.add(APCells.chargedCellStack(APCells.SUPER_DENSE_ITEM, SuperDenseEnergyCellBlock.MAX_POWER));
            charged.add(APCells.chargedCellStack(APCells.EXTREME_DENSE_ITEM, ExtremeDenseEnergyCellBlock.MAX_POWER));
            var manager = jeiRuntime.getIngredientManager();
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, empty);
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, charged);
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("JEI energy-cell ingredient filter skipped: {}", t.toString());
        }
    }
}
