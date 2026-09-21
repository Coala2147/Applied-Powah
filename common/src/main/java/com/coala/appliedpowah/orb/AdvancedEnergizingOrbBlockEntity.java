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
 *   <li>4 rod slots × up to 16 AP rods; all rods must be the same energy family
 *       (all AE or all ME); different tiers are allowed.</li>
 *   <li>Energy cache = Σ count × rod capacity. Rods create/expand this cache.</li>
 *   <li>Network pull fills that cache (AE2 energy-cell / wireless-terminal style),
 *       not just the current recipe remainder.</li>
 *   <li>Recipes consume from the cache; leftover energy stays for the next craft.</li>
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
            // Family lock: all occupied slots must share AE or ME.
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
            // Internal buffer is always FE; AE display divides by 2 later.
            long per = rod.getTier().capacityFe;
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
        long rodCap = rodCapacitySum();
        long show = isAeDisplay() ? rodCap / 2 : rodCap;
        if (show > 0) {
            return show;
        }
        long recipeCap = isAeDisplay() && recipeEnergy > 0 ? recipeEnergy / 2 : recipeEnergy;
        return Math.max(recipeCap, 0);
    }

    /**
     * Rod feed fills the rod-defined cache (not only current recipe).
     * ME orb override stays recipe-only; Advanced is cache-based.
     */
    @Override
    public long fillEnergy(long amount) {
        if (level == null || amount <= 0 || !rodsPresent()) {
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
        setChanged();
        markForUpdate();
        return filled;
    }

    /**
     * Network pull fills the rod cache like AE2 cells / wireless terminals drawing
     * from the grid — independent of whether a recipe is currently loaded.
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
            // Cache full — still try to finish a ready recipe.
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
        if (recipe == null) {
            checkRecipe();
        }
        if (recipe != null && recipeEnergy > 0 && bufferFe >= recipeEnergy) {
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
