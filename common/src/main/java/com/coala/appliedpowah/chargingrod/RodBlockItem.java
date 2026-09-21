package com.coala.appliedpowah.chargingrod;

import com.coala.appliedpowah.client.TooltipStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Rod block item. Spec lines describe tier buffer/output only (AE2 light-gray).
 * Does not fabricate live stored energy and does not add informal source commentary.
 */
public class RodBlockItem extends BlockItem {
    private final RodTier tier;
    private final boolean aeUnit;

    public RodBlockItem(Block block, Properties properties, RodTier tier, boolean aeUnit) {
        super(block, properties);
        this.tier = tier;
        this.aeUnit = aeUnit;
    }

    public RodTier getTier() {
        return tier;
    }

    public boolean isAeUnit() {
        return aeUnit;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        String unit = aeUnit ? "AE" : "FE";
        // Powah-style wording; AE rod values are FE/2 (1 AE = 2 FE). Spec only — no live BE energy.
        long capacity = aeUnit ? tier.capacityFe / 2 : tier.capacityFe;
        long transfer = aeUnit ? tier.transferFe / 2 : tier.transferFe;
        lines.add(Component.translatable("applied_powah.tooltip.rod.buffer",
                        TooltipStyle.formatAmount(capacity), unit)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("applied_powah.tooltip.rod.transfer",
                        TooltipStyle.formatAmount(transfer), unit)
                .withStyle(ChatFormatting.GRAY));
    }
}
