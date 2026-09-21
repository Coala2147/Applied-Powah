package com.coala.appliedpowah.energycell;

import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.block.networking.EnergyCellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/** Shared AE cell block: creative pick-block copies BE power into item NBT (AE2 key names). */
public abstract class APDenseEnergyCellBlock extends EnergyCellBlock {

    protected APDenseEnergyCellBlock(double maxPower, double chargeRate, int priority) {
        super(maxPower, chargeRate, priority);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergyCellBlockEntity cell) {
            var tag = stack.getOrCreateTag();
            double current = cell.getAECurrentPower();
            double max = cell.getAEMaxPower();
            if (current > 0) {
                tag.putDouble("internalCurrentPower", current);
            }
            if (max > 0) {
                tag.putDouble("internalMaxPower", max);
            }
        }
        return stack;
    }
}
