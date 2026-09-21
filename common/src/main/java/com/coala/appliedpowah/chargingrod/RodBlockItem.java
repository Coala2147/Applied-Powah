package com.coala.appliedpowah.chargingrod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Rod block item. No fabricated energy tooltip — the item does not store power.
 * Network and orb behavior is documented in the GuideME book.
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
}
