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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Rod item tooltip: buffer/transfer specs. Label gray, value darker (Powah-style).
 * No informal "source" lines. Live energy lives on the placed BE / drop NBT.
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
        long capacity = aeUnit ? tier.capacityFe / 2 : tier.capacityFe;
        long transfer = aeUnit ? tier.transferFe / 2 : tier.transferFe;
        lines.add(TooltipStyle.labeled("applied_powah.tooltip.rod.buffer_label",
                TooltipStyle.formatAmount(capacity) + " " + unit));
        lines.add(TooltipStyle.labeled("applied_powah.tooltip.rod.transfer_label",
                TooltipStyle.formatAmount(transfer) + " " + unit + "/t"));
        // If this item carries a mined buffer (Powah-style drop), show stored amount.
        var tag = stack.getTag();
        if (tag != null && tag.contains("ap_buffer_fe")) {
            long fe = tag.getLong("ap_buffer_fe");
            long show = aeUnit ? fe / 2 : fe;
            long maxShow = aeUnit ? tier.capacityFe / 2 : tier.capacityFe;
            lines.add(TooltipStyle.storedEnergy(show, maxShow));
        }
    }
}
