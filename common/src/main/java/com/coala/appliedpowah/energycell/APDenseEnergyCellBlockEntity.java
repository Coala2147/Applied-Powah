package com.coala.appliedpowah.energycell;

import appeng.blockentity.networking.EnergyCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy cell BE for Applied Powah.
 *
 * AE2 {@link EnergyCellBlockEntity} does not stream exact power to the client
 * ({@code writeToStream} is empty; only blockstate fullness 0–4 syncs).
 * Creative pick-block runs on the client, so we append power to the AE2 update
 * stream and {@link #markForUpdate()} when the server amount changes.
 */
public class APDenseEnergyCellBlockEntity extends EnergyCellBlockEntity {

    /** Client-side last synced AE; NaN until the first update packet. */
    private double clientPower = Double.NaN;
    private double lastSentPower = Double.NaN;

    public APDenseEnergyCellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Server ticker: push a BE update when stored AE changes. */
    public void serverSyncTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        double cur = getAECurrentPower();
        if (Double.isNaN(lastSentPower) || cur != lastSentPower) {
            lastSentPower = cur;
            markForUpdate();
        }
    }

    /** Power to write onto pick-block item NBT. */
    public double pickPower() {
        if (!Double.isNaN(clientPower)) {
            return clientPower;
        }
        return getAECurrentPower();
    }

    public double pickMaxPower() {
        double max = getAEMaxPower();
        return max > 0 ? max : 0;
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeDouble(getAECurrentPower());
        data.writeDouble(getAEMaxPower());
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean output = super.readFromStream(data);
        if (data.readableBytes() >= 16) {
            clientPower = data.readDouble();
            data.readDouble(); // max, tooltip uses block max
            output = true;
        }
        return output;
    }
}
