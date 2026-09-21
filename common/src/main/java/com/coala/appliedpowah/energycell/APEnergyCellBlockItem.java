package com.coala.appliedpowah.energycell;

import appeng.block.networking.EnergyCellBlock;
import appeng.block.AEBaseBlockItem;
import com.coala.appliedpowah.client.TooltipStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Energy cell item — stores/shows AE power like AE2 EnergyCellBlockItem.
 * NBT key: internalCurrentPower (same as AE2).
 * Tooltip sentence follows AE2 {@code Tooltips.energyStorageComponent} wording;
 * body text is light gray (ChatFormatting.GRAY).
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
        lines.add(TooltipStyle.guideHint());
        lines.add(TooltipStyle.storedEnergy(getStoredPower(stack), max));
    }
}
