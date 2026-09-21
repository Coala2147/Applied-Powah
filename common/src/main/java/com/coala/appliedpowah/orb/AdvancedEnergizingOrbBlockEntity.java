package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
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
 * Advanced Energizing Orb.
 * <ul>
 *   <li>4 rod slots × ≤16 AP rods; same AE/ME family; mixed tiers OK.</li>
 *   <li>Internal energy cache = Σ n×C(t) — like AE2 energy cells / portable terminals:
 *       network pull fills the cache; Jade reads {@link #getAECurrentPower()}.</li>
 *   <li>Rod items never enter craft input slots.</li>
 *   <li>Grid: BOTTOM only (matches tested ground cable).</li>
 * </ul>
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
            if (stack.isEmpty() || !(stack.getItem() instanceof RodBlockItem rod)) {
                return false;
            }
            Boolean family = null;
            for (int i = 0; i < ROD_SLOTS; i++) {
                if (i == slot) {
                    continue;
                }
                ItemStack s = rodInv.getStackInSlot(i);
                if (s.isEmpty() || !(s.getItem() instanceof RodBlockItem other)) {
                    continue;
                }
                if (family == null) {
                    family = other.isAeUnit();
                } else if (family != other.isAeUnit()) {
                    return false;
                }
            }
            return family == null || family == rod.isAeUnit();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
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

    /** Σ n_i × C(t_i) in internal FE. */
    public long rodCapacitySum() {
        long sum = 0;
        for (int i = 0; i < ROD_SLOTS; i++) {
            ItemStack s = rodInv.getStackInSlot(i);
            if (s.isEmpty() || !(s.getItem() instanceof RodBlockItem rod)) {
                continue;
            }
            sum += rod.getTier().capacityFe * (long) s.getCount();
        }
        return sum;
    }

    @Override
    public boolean isAeDisplay() {
        return rodsAreAe();
    }

    @Override
    public long getDisplayEnergy() {
        // Cache is the real stored energy (AE family displays FE/2).
        return isAeDisplay() ? bufferFe / 2 : bufferFe;
    }

    @Override
    public long getDisplayCapacity() {
        long rodCap = rodCapacitySum();
        if (rodCap > 0) {
            return isAeDisplay() ? rodCap / 2 : rodCap;
        }
        return recipeEnergy > 0 ? (isAeDisplay() ? recipeEnergy / 2 : recipeEnergy) : 0;
    }

    /** Jade / AE2: report the rod cache like an energy cell. */
    @Override
    public double getAECurrentPower() {
        return getDisplayEnergy();
    }

    @Override
    public double getAEMaxPower() {
        return getDisplayCapacity();
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }

    @Override
    public long fillEnergy(long amount) {
        if (level == null || amount <= 0 || !rodsPresent() || completing) {
            return 0;
        }
        long cacheMax = rodCapacitySum();
        if (cacheMax <= 0) {
            return 0;
        }
        long empty = Math.max(0, cacheMax - bufferFe);
        long filled = Math.min(empty, amount);
        if (filled > 0) {
            bufferFe += filled;
        }
        if (recipe != null && recipeEnergy > 0 && bufferFe >= recipeEnergy) {
            completeIfPossible();
        }
        updateWarning();
        setChanged();
        markForUpdate();
        return filled;
    }

    /**
     * Network pull fills the rod cache (energy-cell / portable-terminal style).
     * Ground cable on DOWN is enough — node must be active.
     */
    @Override
    protected void pullFromNetwork(IGridNode node) {
        if (!APConfig.COMMON.orbPullFromNetwork.get() || !rodsPresent() || node == null || !node.isActive()) {
            return;
        }
        long cacheMax = rodCapacitySum();
        if (cacheMax <= 0) {
            return;
        }
        long room = Math.max(0, cacheMax - bufferFe);
        if (room <= 0) {
            if (recipe != null && recipeEnergy > 0 && bufferFe >= recipeEnergy) {
                completeIfPossible();
            }
            return;
        }
        if (rodsAreAe()) {
            IEnergyService energy = node.getGrid().getEnergyService();
            double max = Math.max(1.0, energy.getMaxStoredPower());
            double reserve = max * APConfig.COMMON.networkReserveRatio.get();
            double available = Math.max(0.0, energy.getStoredPower() - reserve);
            double wantAe = Math.min((double) APConfig.COMMON.aeBurstAe.get(), available);
            double wantFe = wantAe * 2.0;
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
        } else if (ModList.get().isLoaded("appflux")) {
            var storage = node.getGrid().getService(appeng.api.networking.storage.IStorageService.class);
            if (storage == null) {
                return;
            }
            long want = Math.min(APConfig.COMMON.meBurstFe.get(), room);
            if (want <= 0) {
                return;
            }
            long got = FluxBridge.extractFe(storage, want);
            if (got > 0) {
                bufferFe += got;
            }
        }
        if (recipe == null && !completing) {
            checkRecipe();
        }
        if (recipe != null && recipeEnergy > 0 && bufferFe >= recipeEnergy) {
            completeIfPossible();
        }
        updateWarning();
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
