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
 *   <li>Built-in cache = Extreme Dense Energy Cell equivalent (FE), works with zero rods.</li>
 *   <li>Rod capacities stack on top of the built-in cache.</li>
 *   <li>Rod items never enter craft input slots.</li>
 *   <li>Grid: BOTTOM only (matches tested ground cable).</li>
 * </ul>
 */
public class AdvancedEnergizingOrbBlockEntity extends MeEnergizingOrbBlockEntity
        implements net.minecraftforge.energy.IEnergyStorage {

    public static final int ROD_SLOTS = 4;
    public static final int MAX_RODS_PER_SLOT = 16;
    /** ExtremeDenseEnergyCellBlock.MAX_POWER (AE) × 2 = FE. Always present, no rods needed. */
    public static final long BASE_CACHE_FE =
            (long) com.coala.appliedpowah.energycell.ExtremeDenseEnergyCellBlock.MAX_POWER * 2L;

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
        // No rods → built-in cache is FE (user spec).
        return false;
    }

    /** Σ n_i × C(t_i) in internal FE (rod part only). */
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
        return isAeDisplay() ? bufferFe / 2 : bufferFe;
    }

    @Override
    public long getDisplayCapacity() {
        long totalFe = BASE_CACHE_FE + rodCapacitySum();
        return isAeDisplay() ? totalFe / 2 : totalFe;
    }

    @Override
    public double getAECurrentPower() {
        return isAeDisplay() ? bufferFe / 2.0 : bufferFe / 2.0;
    }

    @Override
    public double getAEMaxPower() {
        return (BASE_CACHE_FE + rodCapacitySum()) / 2.0;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }

    private long cacheMaxFe() {
        return BASE_CACHE_FE + rodCapacitySum();
    }

    /** Overflow drain bookkeeping (rod pull-out leaves FE above cache cap). */
    private int overflowTick;

    /**
     * When rods are removed, leftover FE may exceed the new cache cap.
     * 1) Try to push the excess back into the AE network.
     * 2) Otherwise every 20t shed 5% of current FE (min 5% of BASE_CACHE_FE)
     *    until bufferFe ≤ BASE_CACHE_FE; the final step sheds exactly the excess.
     */
    private void handleOverflow(IGridNode node) {
        long cap = cacheMaxFe();
        if (bufferFe <= cap) {
            overflowTick = 0;
            return;
        }
        long excess = bufferFe - cap;
        // 1) push excess back to AE network
        if (node != null && node.isActive()) {
            try {
                IEnergyService energy = node.getGrid().getEnergyService();
                double max = Math.max(1.0, energy.getMaxStoredPower());
                double roomAe = max - energy.getStoredPower();
                double wantAe = Math.min((double) excess / 2.0, roomAe);
                if (wantAe > 0) {
                    double accepted = energy.injectPower(wantAe, Actionable.MODULATE);
                    if (accepted > 0) {
                        bufferFe -= Math.round(accepted * 2.0);
                        setChanged();
                        markForUpdate();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (bufferFe <= cap) {
            overflowTick = 0;
            return;
        }
        // 2) periodic dissipation
        overflowTick++;
        if (overflowTick < 20) {
            return;
        }
        overflowTick = 0;
        long over = bufferFe - BASE_CACHE_FE;
        if (over <= 0) {
            // between BASE and cap (rods still present) — drain down to cap only
            over = bufferFe - cap;
        }
        long minShed = Math.max(1L, BASE_CACHE_FE / 20); // 5% of default cache
        long shed = Math.max(minShed, bufferFe / 20);    // 5% of current
        if (shed >= over) {
            shed = over; // exact step to the target
        }
        bufferFe = Math.max(0L, bufferFe - shed);
        setChanged();
        markForUpdate();
    }

    @Override
    public long fillEnergy(long amount) {
        if (level == null || amount <= 0 || completing) {
            return 0;
        }
        long cacheMax = cacheMaxFe();
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
     * Network pull fills the built-in + rod cache (energy-cell style).
     * Works with or without rods. Ground cable on DOWN is enough.
     */
    @Override
    protected void pullFromNetwork(IGridNode node) {
        // Always handle rod-pull-out overflow first (works even if pull is disabled).
        handleOverflow(node);
        if (!APConfig.COMMON.orbPullFromNetwork.get() || node == null || !node.isActive()) {
            return;
        }
        long cacheMax = cacheMaxFe();
        if (bufferFe > cacheMax) {
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
        } else if (rodsPresent() && rodsAreAe()) {
            // AE rods but appflux path not taken — already handled above.
        } else {
            // No rods and FE family: still pull AE from grid as FE (1 AE = 2 FE).
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
        pushEnergyOut(node);
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

    // ---- FE output like an energy cell: at most 1% of cache cap per tick ----

    /** Max FE this orb may push/extract per tick = 1% of current cache cap. */
    public long maxOutputPerTick() {
        return Math.max(1L, cacheMaxFe() / 100);
    }

    private long extractedThisTick;

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0; // input goes through fillEnergy / network pull
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        long budget = Math.min(maxOutputPerTick() - extractedThisTick, bufferFe);
        budget = Math.min(budget, maxExtract);
        if (budget <= 0) {
            return 0;
        }
        if (!simulate) {
            bufferFe -= budget;
            extractedThisTick += budget;
            setChanged();
            markForUpdate();
        }
        return (int) Math.min(Integer.MAX_VALUE, budget);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, bufferFe);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, cacheMaxFe());
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    /** Reset the per-tick output budget and push up to 1% into AE network. */
    private void pushEnergyOut(IGridNode node) {
        extractedThisTick = 0;
        if (node == null || !node.isActive() || bufferFe <= 0) {
            return;
        }
        long budget = Math.min(maxOutputPerTick(), bufferFe);
        try {
            IEnergyService energy = node.getGrid().getEnergyService();
            double max = Math.max(1.0, energy.getMaxStoredPower());
            double roomAe = max - energy.getStoredPower();
            double wantAe = Math.min((double) budget / 2.0, roomAe);
            if (wantAe <= 0) {
                return;
            }
            double got = energy.injectPower(wantAe, Actionable.MODULATE);
            if (got > 0) {
                bufferFe -= Math.round(got * 2.0);
                setChanged();
                markForUpdate();
            }
        } catch (Throwable ignored) {
        }
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) {
            return net.minecraftforge.common.util.LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(cap, side);
    }
}
