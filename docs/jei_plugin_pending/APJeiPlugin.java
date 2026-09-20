package com.coala.appliedpowah.jei;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.energycell.APCells;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI plugin: info pages for AP items.
 * Crafting recipes are vanilla data — JEI loads them automatically.
 * AP rods are NOT Powah orb (充能) recipes; they are workbench crafts.
 */
@JeiPlugin
public class APJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(AppliedPowah.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
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
                if (stack.getItem() instanceof com.coala.appliedpowah.chargingrod.RodBlockItem rod) {
                    info.add(Component.translatable("jei.applied_powah.rod_craft_note"));
                    info.add(Component.translatable("jei.applied_powah.rod_place_note"));
                } else {
                    info.add(Component.translatable("jei.applied_powah.cell_note"));
                }
                registration.addIngredientInfo(stack, VanillaTypes.ITEM_STACK, info.toArray(new Component[0]));
            }
            AppliedPowah.LOG.info("JEI: registered {} Applied Powah info entries", stacks.size());
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("JEI plugin skipped: {}", t.toString());
        }
    }
}
