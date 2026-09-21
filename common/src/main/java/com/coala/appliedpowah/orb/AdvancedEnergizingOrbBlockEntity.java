package com.coala.appliedpowah.orb;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import com.coala.appliedpowah.chargingrod.RodBlockItem;
import com.coala.appliedpowah.config.APConfig;
import com.coala.appliedpowah.integration.appflux.FluxBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Advanced Energizing Orb — endgame machine in the Powah energizing chain.
 * Temporary craft: 7× nitro Powah rods + ME Energizing Orb + AE2 energy acceptor.
 * 4 rod slots (max 16 AP rods each, same energy family).
 * May pull AE/FE from the network when config allows; unlike the ME orb.
 */
public class AdvancedEnergizingOrbBlockEntity extends MeEnergizingOrbBlockEntity {

    public static final int ROD_SLOTS = 4;
    public static final int MAX_RODS_PER_SLOT = 16;

    private final ItemStackHandler rodInv = new ItemStackHandler(ROD_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_RODS_PER_SLOT;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof RodBlockItem;
        }
    };

    public AdvancedEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStackHandler getRodInv() {
        return rodInv;
    }

    public boolean rodsPresent() {
        for (int i = 0; i < ROD_SLOTS; i++) {
            if (!rodInv.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean rodsAreAe() {
        for (int i = 0; i < ROD_SLOTS; i++) {
            ItemStack s = rodInv.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() instanceof RodBlockItem rod) {
                return rod.isAeUnit();
            }
        }
        return true;
    }

    public long rodCapacitySum() {
        long sum = 0;
        for (int i = 0; i < ROD_SLOTS; i++) {
            ItemStack s = rodInv.getStackInSlot(i);
            if (s.isEmpty() || !(s.getItem() instanceof RodBlockItem rod)) {
                continue;
            }
            long per = rod.isAeUnit() ? rod.getTier().capacityFe / 2 : rod.getTier().capacityFe;
            sum += per * (long) s.getCount();
        }
        return sum;
    }

    @Override
    public boolean isAeDisplay() {
        return rodsAreAe();
    }

    @Override
    public long getDisplayEnergy() {
        // Internal cache is FE; AE family displays as AE (FE/2), matching rods.
        return isAeDisplay() ? bufferFe / 2 : bufferFe;
    }

    @Override
    public long getDisplayCapacity() {
        long recipeCap = isAeDisplay() && recipeEnergy > 0 ? recipeEnergy / 2 : recipeEnergy;
        long rodCap = rodCapacitySum();
        return Math.max(recipeCap, rodCap);
    }

    @Override
    protected void pullFromNetwork(IGridNode node) {
        if (!APConfig.COMMON.orbPullFromNetwork.get() || !rodsPresent() || node == null || !node.isActive()) {
            return;
        }
        if (recipe != null && recipeEnergy > 0 && bufferFe >= recipeEnergy) {
            return;
        }
        long room = recipeEnergy > 0 ? Math.max(0, recipeEnergy - bufferFe) : 0;
        if (room <= 0 && recipe != null) {
            return;
        }
        // Input budget: generous but reserved; Advanced is allowed to drain with reserve
        if (rodsAreAe()) {
            IEnergyService energy = node.getGrid().getEnergyService();
            double max = Math.max(1.0, energy.getMaxStoredPower());
            double reserve = max * APConfig.COMMON.networkReserveRatio.get();
            double available = Math.max(0.0, energy.getStoredPower() - reserve);
            double wantAe = Math.min((double) APConfig.COMMON.aeBurstAe.get(), available);
            double wantFe = wantAe * 2.0;
            if (room > 0) {
                wantFe = Math.min(wantFe, room);
                wantAe = wantFe / 2.0;
            }
            if (wantAe <= 0) {
                return;
            }
            double got = energy.extractAEPower(wantAe, Actionable.MODULATE, PowerMultiplier.ONE);
            if (got > 0) {
                bufferFe += Math.round(got * 2.0);
            }
        } else if (ModList.get().isLoaded("appflux")) {
            var storage = node.getGrid().getService(appeng.api.networking.storage.IStorageService.class);
            if (storage == null) {
                return;
            }
            long want = APConfig.COMMON.meBurstFe.get();
            if (room > 0) {
                want = Math.min(want, room);
            }
            long got = FluxBridge.extractFe(storage, want);
            if (got > 0) {
                bufferFe += got;
            }
        }
        if (recipe == null) {
            checkRecipe();
        }
        if (recipe != null && recipeEnergy > 0 && bufferFe >= recipeEnergy) {
            fillEnergy(0);
            completeIfPossible();
        }
        setChanged();
        markForUpdate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("rodInv", rodInv.serializeNBT());
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains("rodInv")) {
            rodInv.deserializeNBT(tag.getCompound("rodInv"));
        }
    }
}
