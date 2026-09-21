package com.coala.appliedpowah.chargingrod;

import com.coala.appliedpowah.client.TooltipStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Rod block item. Does not fabricate stored energy on the item stack
 * (placed rods hold energy in the BE; mined rods drop empty).
 * Spec lines describe tier capacity / transfer / network source only.
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
        lines.add(TooltipStyle.guideHint());

        String unit = aeUnit ? "AE" : "FE";
        long capacity = aeUnit ? tier.capacityFe / 2 : tier.capacityFe;
        long transfer = aeUnit ? tier.transferFe / 2 : tier.transferFe;
        lines.add(TooltipStyle.spec("applied_powah.tooltip.rod.cache",
                TooltipStyle.formatAmount(capacity), unit));
        lines.add(TooltipStyle.spec("applied_powah.tooltip.rod.output",
                TooltipStyle.formatAmount(transfer), unit));

        if (aeUnit) {
            lines.add(TooltipStyle.spec("applied_powah.tooltip.rod.source_ae"));
        } else if (ModList.get().isLoaded("appflux")) {
            lines.add(TooltipStyle.spec("applied_powah.tooltip.rod.source_me"));
        } else {
            lines.add(TooltipStyle.spec("applied_powah.tooltip.rod.source_me_missing"));
        }
    }
}
