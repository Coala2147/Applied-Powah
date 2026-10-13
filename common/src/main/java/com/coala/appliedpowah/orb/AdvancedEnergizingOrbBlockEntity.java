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
 * Advanced Energizing Orb.
 * <ul>
 *   <li>4 craft pages (explicit threads, ExtendedAE ex-molecular-assembler layout).</li>
 *   <li>4 rod slots × up to 16 AP rods; same AE/ME family; mixed tiers OK.</li>
 *   <li>Built-in cache = Extreme Dense Energy Cell equivalent (FE), works with zero rods.</li>
 *   <li>Rod capacities stack on top of the built-in cache; energy cards scale both
 *       cache cap and network-pull rate (supply bandwidth for acceleration cards).</li>
 *   <li>Rod items never enter craft input slots; rods never beam into this tier.</li>
 * </ul>
 */
public class AdvancedEnergizingOrbBlockEntity extends EnergizingOrbBlockEntity
        implements net.minecraftforge.energy.IEnergyStorage {

    public static final int ROD_SLOTS = 4;
    public static final int MAX_RODS_PER_SLOT = 16;
    /** ExtremeDenseEnergyCellBlock.MAX_POWER (AE) × 2 = FE. Always present, no rods needed. */
    public static final long BASE_CACHE_FE =
            (long) com.coala.appliedpowah.energycell.ExtremeDenseEnergyCellBlock.MAX_POWER * 2L;

    /** Rods must not beam into this tier (rod slots + network pull instead). */
    @Override
    public boolean rodFeedable() {
        return false;
    }

    @Override
    public int pageCount() {
        return PAGES;
    }

    /** Explicit 4 threads for Advanced / ME tiers (issues.md 其他-10). */
    public static final int PAGES = 4;

    private final ItemStackHandler rodInv = new ItemStackHandler(ROD_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            handleOverflowNow();
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
        long totalFe = cacheMaxFe();
        return isAeDisplay() ? totalFe / 2 : totalFe;
    }

    @Override
    public double getAECurrentPower() {
        return bufferFe / 2.0;
    }

    @Override
    public double getAEMaxPower() {
        return cacheMaxFe() / 2.0;
    }

    @Override
    public appeng.api.config.AccessRestriction getPowerFlow() {
        return appeng.api.config.AccessRestriction.NO_ACCESS;
    }

    private long cacheMaxFe() {
        return OrbTuning.cacheMax(BASE_CACHE_FE + rodCapacitySum(), energyCardCount());
    }

    /** Keep ticking (and pulling) until the energy-cell cache is full. */
    public boolean wantsCacheFill() {
        return bufferFe < cacheMaxFe();
    }

    // ---- overflow drain bookkeeping (rod pull-out leaves FE above cache cap) ----

    private int overflowTick;

    private void handleOverflowNow() {
        handleOverflow(getMainNode().getNode());
    }

    /**
     * When rods are removed, leftover FE may exceed the new cache cap.
     * 1) Try to push the excess back into the AE network.
     * 2) Otherwise every 20t shed 5% of current FE (min 5% of BASE_CACHE_FE)
     *    until bufferFe ≤ cap; the final step sheds exactly the excess.
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
            } catch (Exception e) {
                com.coala.appliedpowah.AppliedPowah.LOG.debug("handleOverflow inject failed: {}", e.toString());
            }
        }
        if (bufferFe <= cap) {
            overflowTick = 0;
            return;
        }
        // 2) periodic dissipation — shed only above the *current* cap (rods may still provide room)
        overflowTick++;
        if (overflowTick < 20) {
            return;
        }
        overflowTick = 0;
        long over = OrbEnergyMath.overflowAbove(bufferFe, cap);
        if (over <= 0) {
            return;
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

    /**
     * Network pull fills recipe progress first, then the built-in + rod cache.
     * A full cache must not block a high-cost recipe. Works with or without rods.
     */
    @Override
    protected void pullFromNetwork(IGridNode node) {
        // Always handle rod-pull-out overflow first (works even if pull is disabled).
        handleOverflow(node);
        if (!APConfig.COMMON.orbPullFromNetwork.get() || node == null || !node.isActive()) {
            feedPagesFromBuffer();
            return;
        }
        long cacheMax = cacheMaxFe();
        long cacheRoom = OrbEnergyMath.cacheRoom(bufferFe, cacheMax);
        long recipeNeed = recipeNeed();
        // Still try to complete from existing progress / buffer even with no pull room.
        if (cacheRoom <= 0 && recipeNeed <= 0) {
            feedPagesFromBuffer();
            completeReady();
            return;
        }
        long room = cacheRoom + recipeNeed; // may pull past cache cap when a recipe needs it
        long rateCap = OrbTuning.rateLimit(cacheMaxFe(), OrbTuning.rIn(energyCardCount()));
        if (rodsAreAe()) {
            extractAeFromGrid(node, room, rateCap);
        } else if (ModList.get().isLoaded("appflux")) {
            var storage = node.getGrid().getService(appeng.api.networking.storage.IStorageService.class);
            if (storage == null) {
                feedPagesFromBuffer();
                return;
            }
            long want = Math.min(APConfig.COMMON.meBurstFe.get(), Math.min(room, rateCap > 0 ? rateCap : room));
            if (want <= 0) {
                feedPagesFromBuffer();
                return;
            }
            long got = FluxBridge.extractFe(storage, want);
            if (got > 0) {
                bufferFe += got;
            }
        } else {
            // No rods (or no appflux): still pull AE from grid as FE (1 AE = 2 FE).
            extractAeFromGrid(node, room, rateCap);
        }
        feedPagesFromBuffer();
        completeReady();
        updateWarning();
        setChanged();
        markForUpdate();
        pushEnergyOut(node);
    }

    private void extractAeFromGrid(IGridNode node, long room, long rateCap) {
        IEnergyService energy = node.getGrid().getEnergyService();
        double max = Math.max(1.0, energy.getMaxStoredPower());
        double reserve = max * APConfig.COMMON.networkReserveRatio.get();
        double available = Math.max(0.0, energy.getStoredPower() - reserve);
        double wantAe = Math.min((double) APConfig.COMMON.aeBurstAe.get(), available);
        double wantFe = wantAe * 2.0;
        long capFe = Math.min(room, rateCap > 0 ? rateCap : room);
        if (wantFe > capFe) {
            wantFe = capFe;
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

    /** FE still missing across all loaded pages. */
    private long recipeNeed() {
        long need = 0;
        for (CraftThread page : threads) {
            need += page.remaining();
        }
        return need;
    }

    private void completeReady() {
        for (CraftThread page : threads) {
            if (page.ready()) {
                completeIfPossible(page);
            }
        }
    }

    @Override
    public long fillEnergy(long amount) {
        if (level == null || amount <= 0 || settling) {
            return 0;
        }
        long filled = routeEnergyToPages(amount, 1);
        long rest = amount - filled;
        if (rest > 0) {
            long room = OrbEnergyMath.cacheRoom(bufferFe, cacheMaxFe());
            long toCache = Math.min(room, rest);
            if (toCache > 0) {
                bufferFe += toCache;
                filled += toCache;
            }
        }
        completeReady();
        updateWarning();
        setChanged();
        markForUpdate();
        return filled;
    }

    @Override
    protected boolean wantsWork() {
        return wantsCacheFill() || rodsPresent();
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

    // ---- FE output like an energy cell: at most r_out of cache cap per tick ----

    /** Max FE this orb may push/extract per tick = r_out × cache. */
    public long maxOutputPerTick() {
        return OrbTuning.rateLimit(cacheMaxFe(), OrbTuning.rOut(energyCardCount()));
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
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, bufferFe));
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

    /** Reset the per-tick output budget and push up to r_out into AE network. */
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
        } catch (Exception e) {
            com.coala.appliedpowah.AppliedPowah.LOG.debug("pushEnergyOut failed: {}", e.toString());
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
