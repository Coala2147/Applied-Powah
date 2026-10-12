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
import net.minecraftforge.fml.ModList;
import owmii.powah.block.energizing.EnergizingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI: AP rods + orbs as catalysts on Powah energizing;
 * energy cells: empty only in JEI index (charged creative variants hidden).
 *
 * <p>Powah is a soft dependency. JEI instantiates this plugin whenever JEI is
 * installed, so the {@code EnergizingRecipe} class literal must NOT sit in a
 * static initializer — it would throw NoClassDefFoundError when Powah is
 * absent. Every Powah-touching path is therefore guarded by
 * {@link ModList#isLoaded(String)} and resolves lazily inside method bodies.</p>
 */
@JeiPlugin
public class APJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(AppliedPowah.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("powah")) {
            // No powah:energizing recipe type exists without Powah — skip cleanly.
            return;
        }
        try {
            // Created lazily: resolves EnergizingRecipe only now that Powah is present.
            RecipeType<EnergizingRecipe> powahEnergizing =
                    RecipeType.create("powah", "energizing", EnergizingRecipe.class);
            APCells.AE_RODS.forEach((tier, block) ->
                    registration.addRecipeCatalyst(new ItemStack(block.get()), powahEnergizing));
            APCells.ME_RODS.forEach((tier, block) ->
                    registration.addRecipeCatalyst(new ItemStack(block.get()), powahEnergizing));
            if (APOrbs.FLUIX_ORB_ITEM != null && APOrbs.FLUIX_ORB_ITEM.isPresent()) {
                registration.addRecipeCatalyst(new ItemStack(APOrbs.FLUIX_ORB_ITEM.get()), powahEnergizing);
            }
            if (APOrbs.ME_ORB_ITEM != null && APOrbs.ME_ORB_ITEM.isPresent()) {
                registration.addRecipeCatalyst(new ItemStack(APOrbs.ME_ORB_ITEM.get()), powahEnergizing);
            }
            if (APOrbs.ADV_ORB_ITEM != null && APOrbs.ADV_ORB_ITEM.isPresent()) {
                registration.addRecipeCatalyst(new ItemStack(APOrbs.ADV_ORB_ITEM.get()), powahEnergizing);
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
