package com.coala.appliedpowah.jei;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.energycell.APCells;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import owmii.powah.block.energizing.EnergizingRecipe;

/**
 * Registers Applied Powah rods as catalysts on Powah's Energizing category
 * (uid {@code powah:energizing}). Same role as Powah's own rods: they supply
 * the orb; they are not crafting ingredients.
 * No long ingredient-info essays — details belong in GuideME.
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
            AppliedPowah.LOG.info("JEI: AP rods registered as powah:energizing catalysts");
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("JEI catalyst registration skipped: {}", t.toString());
        }
    }
}
