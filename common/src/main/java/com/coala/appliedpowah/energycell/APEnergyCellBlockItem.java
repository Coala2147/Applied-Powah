package com.coala.appliedpowah.energycell;

import appeng.block.networking.EnergyCellBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Energy cell item — extends AE2's {@link EnergyCellBlockItem} so the mechanism
 * matches stock AE2 cells:
 * <ul>
 * <li>{@code IAEItemPowerStorage} (inject/extract on the item, charger charge-rate)</li>
 * <li>AE2 {@code Tooltips.energyStorageComponent} wording</li>
 * <li>client item model property {@code ae2:fill_level} (registered by AE2 for every
 * {@code EnergyCellBlockItem})</li>
 * </ul>
 * NBT key {@code internalCurrentPower} is the same as AE2.
 */
public class APEnergyCellBlockItem extends EnergyCellBlockItem {

    public APEnergyCellBlockItem(Block block, Item.Properties props) {
        super(block, props);
    }
}
