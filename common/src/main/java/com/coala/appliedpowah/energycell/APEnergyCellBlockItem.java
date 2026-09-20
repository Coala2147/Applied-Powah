package com.coala.appliedpowah.energycell;

import appeng.block.networking.EnergyCellBlock;
import appeng.block.AEBaseBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;

/**
 * Energy cell item — saves/shows AE power like AE2 EnergyCellBlockItem.
 * NBT key: internalCurrentPower (same as AE2).
 */
public class APEnergyCellBlockItem extends AEBaseBlockItem {

    public APEnergyCellBlockItem(Block block, Item.Properties props) {
        super(block, props);
    }

    public double getMaxEnergyCapacity() {
        return ((EnergyCellBlock) getBlock()).getMaxPower();
    }

    public double getStoredPower(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? 0 : tag.getDouble("internalCurrentPower");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addCheckedInformation(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        double max = getMaxEnergyCapacity();
        if (max <= 0) {
            return;
        }
        double cur = getStoredPower(stack);
        lines.add(Component.literal(formatAe(cur) + " / " + formatAe(max) + " AE"));
    }

    private static String formatAe(double v) {
        if (v >= 1_000_000_000D) {
            return trim(v / 1_000_000_000D) + "G";
        }
        if (v >= 1_000_000D) {
            return trim(v / 1_000_000D) + "M";
        }
        if (v >= 1_000D) {
            return trim(v / 1_000D) + "k";
        }
        return trim(v);
    }

    private static String trim(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
