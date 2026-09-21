package com.coala.appliedpowah.energycell;

import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.block.networking.EnergyCellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Shared AE cell block.
 *
 * Creative pick-block (Forge patches {@code Minecraft.pickBlock} to
 * {@code BlockState.getCloneItemStack(hit, level, pos, player)} →
 * {@link EnergyCellBlock#getCloneItemStack}). Both the 5-arg and deprecated
 * 3-arg overloads are implemented because callers differ across mappings.
 *
 * Client BE power may be 0 without sync; {@link APDenseEnergyCellBlockEntity}
 * streams AE. Item NBT uses AE2 keys {@code internalCurrentPower} /
 * {@code internalMaxPower} so dismantle/place paths stay compatible.
 */
public abstract class APDenseEnergyCellBlock extends EnergyCellBlock {

    protected APDenseEnergyCellBlock(double maxPower, double chargeRate, int priority) {
        super(maxPower, chargeRate, priority);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof APDenseEnergyCellBlockEntity cell) {
                cell.serverSyncTick();
            }
        };
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return applyPickNbt(super.getCloneItemStack(state, target, level, pos, player), level, pos);
    }

    @Override
    @Deprecated
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return applyPickNbt(super.getCloneItemStack(level, pos, state), level, pos);
    }

    private ItemStack applyPickNbt(ItemStack stack, BlockGetter level, BlockPos pos) {
        if (stack.isEmpty()) {
            stack = new ItemStack(this);
        }
        BlockEntity be = level.getBlockEntity(pos);
        double current = 0;
        double max = getMaxPower();
        if (be instanceof APDenseEnergyCellBlockEntity ap) {
            current = ap.pickPower();
            double m = ap.pickMaxPower();
            if (m > 0) {
                max = m;
            }
        } else if (be instanceof EnergyCellBlockEntity cell) {
            current = cell.getAECurrentPower();
            double m = cell.getAEMaxPower();
            if (m > 0) {
                max = m;
            }
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (current > 0) {
            tag.putDouble("internalCurrentPower", current);
        }
        if (max > 0) {
            tag.putDouble("internalMaxPower", max);
        }
        // Also keep a BlockEntityTag copy so vanilla ctrl-pick / place can restore BE state.
        if (be != null) {
            try {
                CompoundTag beTag = be.saveWithFullMetadata();
                beTag.remove("x");
                beTag.remove("y");
                beTag.remove("z");
                beTag.remove("id");
                tag.put("BlockEntityTag", beTag);
            } catch (Throwable ignored) {
            }
        }
        return stack;
    }
}
