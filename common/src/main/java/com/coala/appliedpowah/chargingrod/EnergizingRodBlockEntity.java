package com.coala.appliedpowah.chargingrod;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.coala.appliedpowah.config.APConfig;
import com.coala.appliedpowah.integration.appflux.FluxBridge;
import com.coala.appliedpowah.integration.powah.PowahBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;

import java.util.EnumSet;
import java.util.Set;

/** Full-block rod BE. Connects to AE2 on FACING side only. Beam renders toward orbPos. */
public class EnergizingRodBlockEntity extends AENetworkBlockEntity implements IGridTickable, IEnergyStorage {

    private long bufferFe;
    private int pullAccum;
    private BlockPos orbPos = BlockPos.ZERO;
    private long lastPushed;

    public EnergizingRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode()
                .setIdlePowerUsage(1.0)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                // AE2 ME controller / network tool shows this icon+name; without it → Air.
                .setVisualRepresentation(getItemFromBlockEntity());
    }

    /**
     * All rod blocks share one BlockEntityType, so AE2's REPRESENTATIVE_ITEMS map
     * cannot distinguish tiers. Resolve the item from the block instead of Items.AIR.
     */
    @Override
    protected net.minecraft.world.item.Item getItemFromBlockEntity() {
        try {
            return getBlockState().getBlock().asItem();
        } catch (Exception e) {
            return super.getItemFromBlockEntity();
        }
    }

    public RodTier tier() {
        return ((EnergizingRodBlock) getBlockState().getBlock()).getTier();
    }

    public boolean isAe() {
        return ((EnergizingRodBlock) getBlockState().getBlock()).isAeUnit();
    }

    public Direction facing() {
        return getBlockState().getValue(EnergizingRodBlock.FACING);
    }

    public long getBufferFe() {
        return bufferFe;
    }

    public BlockPos getOrbPos() {
        return orbPos;
    }

    public boolean isBeaming() {
        return lastPushed > 0 && !orbPos.equals(BlockPos.ZERO);
    }

    /** Powah-style item NBT key for stored FE buffer. */
    public static final String NBT_BUFFER_FE = "ap_buffer_fe";

    public net.minecraft.world.item.ItemStack writeBufferToStack(net.minecraft.world.item.ItemStack stack) {
        if (bufferFe > 0) {
            stack.getOrCreateTag().putLong(NBT_BUFFER_FE, bufferFe);
        }
        return stack;
    }

    public void readBufferFromStack(net.minecraft.world.item.ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null && tag.contains(NBT_BUFFER_FE)) {
            RodTier t = tier();
            bufferFe = Math.max(0, Math.min(t.capacityFe, tag.getLong(NBT_BUFFER_FE)));
            setChanged();
        }
    }

    public long getDisplayEnergy() {
        return isAe() ? bufferFe / 2 : bufferFe;
    }

    public long getDisplayCapacity() {
        RodTier t = tier();
        return isAe() ? t.capacityFe / 2 : t.capacityFe;
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        try {
            return EnumSet.of(facing());
        } catch (Exception e) {
            return EnumSet.allOf(Direction.class);
        }
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        try {
            return dir == facing() ? AECableType.COVERED : AECableType.NONE;
        } catch (Exception e) {
            return AECableType.COVERED;
        }
    }

    public void onFacingMaybeChanged() {
        onGridConnectableSidesChanged();
        scanForOrb();
    }

    public void scanForOrb() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos found = PowahBridge.findNearbyOrb(level, worldPosition, orbPos);
        orbPos = found == null ? BlockPos.ZERO : found;
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("bufferFe", bufferFe);
        tag.putInt("orbX", orbPos.getX());
        tag.putInt("orbY", orbPos.getY());
        tag.putInt("orbZ", orbPos.getZ());
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        bufferFe = tag.getLong("bufferFe");
        orbPos = new BlockPos(tag.getInt("orbX"), tag.getInt("orbY"), tag.getInt("orbZ"));
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        lastPushed = 0;
        if (level == null || level.isClientSide() || !node.isActive()) {
            return TickRateModulation.SLEEP;
        }

        int elapsed = Math.max(1, ticksSinceLastCall);
        int interval = Math.max(1, APConfig.COMMON.pullIntervalTicks.get());
        pullAccum += elapsed;
        while (pullAccum >= interval) {
            pullAccum -= interval;
            if (isAe()) {
                pullAe(node.getGrid());
            } else {
                pullFe(node.getGrid());
            }
        }
        feedOrb();
        return TickRateModulation.IDLE;
    }

    private void pullAe(IGrid grid) {
        RodTier t = tier();
        if (bufferFe >= t.capacityFe) {
            return;
        }
        IEnergyService energy = grid.getEnergyService();
        double max = Math.max(1.0, energy.getMaxStoredPower());
        double stored = energy.getStoredPower();
        double reserve = max * APConfig.COMMON.networkReserveRatio.get();
        double available = Math.max(0.0, stored - reserve);
        double wantAe = Math.min((double) APConfig.COMMON.aeBurstAe.get(), available);
        double wantFe = wantAe * 2.0;
        double room = t.capacityFe - bufferFe;
        if (wantFe > room) {
            wantFe = room;
            wantAe = wantFe / 2.0;
        }
        if (wantAe <= 0) {
            return;
        }
        double got = energy.extractAEPower(wantAe, Actionable.MODULATE, PowerMultiplier.ONE);
        if (got > 0) {
            bufferFe += Math.round(got * 2.0);
        }
    }

    private void pullFe(IGrid grid) {
        RodTier t = tier();
        if (bufferFe >= t.capacityFe || !ModList.get().isLoaded("appflux")) {
            return;
        }
        IStorageService storage = grid.getService(IStorageService.class);
        if (storage == null) {
            return;
        }
        long want = Math.min(APConfig.COMMON.meBurstFe.get(), t.capacityFe - bufferFe);
        if (want <= 0) {
            return;
        }
        long got = FluxBridge.extractFe(storage, want);
        if (got > 0) {
            bufferFe += got;
        }
    }

    private void feedOrb() {
        if (bufferFe <= 0 || level == null || !ModList.get().isLoaded("powah")) {
            return;
        }
        RodTier t = tier();
        long pushed = PowahBridge.feedNearbyOrb(level, worldPosition, orbPos, bufferFe, t.transferFe);
        if (pushed > 0) {
            bufferFe -= pushed;
            lastPushed = pushed;
            if (orbPos.equals(BlockPos.ZERO)) {
                scanForOrb();
            }
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getDisplayEnergy());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getDisplayCapacity());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
