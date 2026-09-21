package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.coala.appliedpowah.config.APConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import owmii.powah.block.energizing.EnergizingRecipe;

import java.util.EnumSet;
import java.util.Set;

/**
 * ME Energizing Orb.
 * <ul>
 *   <li><b>No energy cache.</b> Never pulls AE/FE from the ME network.</li>
 *   <li>Rod-fed only: {@link #fillEnergy} advances the <em>current recipe</em>.</li>
 *   <li>Has GUI + optional auto-export to ME / adjacent inventories
 *       (AE2 Interface-style insert into {@link IStorageService}).</li>
 *   <li>Grid connection: BOTTOM only.</li>
 * </ul>
 * {@link IAEPowerStorage} is implemented but always reports 0/0 so Jade does not
 * invent a fake cache. Progress lives on the recipe buffer only.
 */
public class MeEnergizingOrbBlockEntity extends AENetworkBlockEntity
        implements IGridTickable, EnergyAcceptingOrb, IAEPowerStorage {

    public static final String NBT_EXPORT = "ap_orb_auto_export";

    protected final ItemStackHandler inv = new ItemStackHandler(EnergizingOrbLogic.SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            checkRecipe();
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == EnergizingOrbLogic.OUTPUT
                    || EnergizingOrbLogic.canInsertInput(this, slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            // Output stacks to 64; inputs are single-item catalysts (Powah rule).
            return slot == EnergizingOrbLogic.OUTPUT ? 64 : 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != EnergizingOrbLogic.OUTPUT && stack.getCount() > 1) {
                // Never accept a multi-count into a 1-slot input (prevents "1 in → 64 out" confusion).
                ItemStack single = stack.copy();
                single.setCount(1);
                ItemStack left = super.insertItem(slot, single, simulate);
                if (simulate) {
                    return stack;
                }
                if (left.isEmpty()) {
                    ItemStack rest = stack.copy();
                    rest.shrink(1);
                    return rest;
                }
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot != EnergizingOrbLogic.OUTPUT && stack.getCount() > 1) {
                ItemStack single = stack.copy();
                single.setCount(1);
                super.setStackInSlot(slot, single);
                return;
            }
            super.setStackInSlot(slot, stack);
        }
    };

    /** Recipe progress buffer only — ME orb has no independent energy cache. */
    protected long bufferFe;
    protected long recipeEnergy;
    protected boolean containRecipe;
    protected boolean autoExport;
    @Nullable
    protected EnergizingRecipe recipe;
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();

    public MeEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode()
                .setIdlePowerUsage(0)
                .setExposedOnSides(EnumSet.of(Direction.DOWN))
                .addService(IGridTickable.class, this);
        try {
            autoExport = APConfig.COMMON.orbAutoExport.get();
        } catch (Throwable t) {
            autoExport = false;
        }
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    public boolean isAutoExport() {
        return autoExport;
    }

    public void setAutoExport(boolean value) {
        this.autoExport = value;
        setChanged();
        markForUpdate();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    public void toggleAutoExport() {
        setAutoExport(!autoExport);
    }

    public long getProgress() {
        return recipeEnergy <= 0 ? 0 : Math.min(recipeEnergy, bufferFe);
    }

    public long getRecipeEnergy() {
        return recipeEnergy;
    }

    public long getBufferFe() {
        return bufferFe;
    }

    @Override
    public boolean containRecipe() {
        return containRecipe;
    }

    /**
     * ME orb: energy only advances the active recipe (rod-fed).
     * Advanced overrides this to fill the rod cache instead.
     */
    @Override
    public long fillEnergy(long amount) {
        if (level == null || recipe == null || recipeEnergy <= 0 || amount <= 0) {
            return 0;
        }
        long empty = Math.max(0, recipeEnergy - bufferFe);
        long filled = Math.min(empty, amount);
        if (filled > 0) {
            bufferFe += filled;
        }
        if (bufferFe >= recipeEnergy) {
            completeIfPossible();
        }
        setChanged();
        markForUpdate();
        return filled;
    }

    /**
     * Finish one Powah craft:
     * <ul>
     *   <li>Result count = recipe result (never invent 64).</li>
     *   <li>Output slot may stack same item up to 64.</li>
     *   <li>Consume only {@code recipeEnergy} from the buffer (Advanced keeps cache).</li>
     *   <li>If output is full / different item — wait; do not eat inputs.</li>
     * </ul>
     */
    protected void completeIfPossible() {
        if (level == null || recipe == null || recipeEnergy <= 0 || bufferFe < recipeEnergy) {
            return;
        }
        ItemStack out = EnergizingOrbLogic.resultOf(level, recipe);
        if (out.isEmpty()) {
            return;
        }
        // Defensive: recipe result count only; clamp to slot stack size 64.
        int outCount = out.getCount();
        if (outCount <= 0) {
            return;
        }
        if (outCount > 64) {
            out.setCount(64);
            outCount = 64;
        }
        ItemStack slot = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
        if (slot.isEmpty()) {
            inv.setStackInSlot(EnergizingOrbLogic.OUTPUT, out);
        } else if (ItemStack.isSameItemSameTags(slot, out)
                && slot.getCount() + outCount <= 64) {
            ItemStack merged = slot.copy();
            merged.grow(outCount);
            inv.setStackInSlot(EnergizingOrbLogic.OUTPUT, merged);
        } else {
            return;
        }
        EnergizingOrbLogic.clearInputs(inv);
        // Consume recipe energy only — Advanced cache may retain leftover.
        bufferFe = Math.max(0, bufferFe - recipeEnergy);
        recipeEnergy = 0;
        recipe = null;
        containRecipe = false;
        setChanged();
        markForUpdate();
    }

    protected void checkRecipe() {
        if (level == null || level.isClientSide) {
            return;
        }
        EnergizingRecipe found = EnergizingOrbLogic.findRecipe(level, inv);
        if (found != null) {
            if (recipe != found) {
                // New recipe: ME orb has no cache — reset progress.
                // Advanced may keep buffer (override via fill path); here reset when recipe identity changes.
                if (!(this instanceof AdvancedEnergizingOrbBlockEntity)) {
                    bufferFe = 0;
                }
            }
            recipe = found;
            recipeEnergy = found.getEnergy();
            containRecipe = true;
            if (bufferFe >= recipeEnergy) {
                completeIfPossible();
            }
        } else {
            recipe = null;
            recipeEnergy = 0;
            if (!(this instanceof AdvancedEnergizingOrbBlockEntity)) {
                bufferFe = 0;
            }
            containRecipe = false;
        }
        setChanged();
        markForUpdate();
    }

    /** ME orb: no cache display. */
    public boolean isAeDisplay() {
        return false;
    }

    public long getDisplayEnergy() {
        // ME orb: no energy cache — only show in-progress recipe buffer.
        return recipeEnergy > 0 ? bufferFe : 0;
    }

    public long getDisplayCapacity() {
        // 0 → hide Jade "Stored" line when idle (no fake cache).
        return recipeEnergy > 0 ? recipeEnergy : 0;
    }

    public String getEnergyUnit() {
        return isAeDisplay() ? "AE" : "FE";
    }

    // ---- AE2 IAEPowerStorage — ME orb intentionally reports no cache ----

    @Override
    public double getAECurrentPower() {
        return 0;
    }

    @Override
    public double getAEMaxPower() {
        return 0;
    }

    @Override
    public double injectAEPower(double amt, Actionable mode) {
        return amt;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        return 0;
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return false;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }

    /** ME orb: absolute — never extract AE/FE from the network. */
    protected void pullFromNetwork(IGridNode node) {
    }

    protected int autoExportBudget() {
        try {
            return Math.max(1, APConfig.COMMON.orbExportItemsPerTick.get());
        } catch (Throwable t) {
            return 8;
        }
    }

    protected boolean autoExportEnabled() {
        return autoExport;
    }

    /**
     * Auto-export finished products into ME storage (Interface-style) and/or
     * adjacent inventories. Rate-limited.
     */
    protected void autoExportTick(IGridNode node) {
        if (!autoExportEnabled() || level == null || level.isClientSide) {
            return;
        }
        int budget = autoExportBudget();
        ItemStack out = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
        if (out.isEmpty()) {
            return;
        }
        // Adjacent inventories first (ExInscriber pushOutResult pattern)
        for (Direction dir : Direction.values()) {
            if (budget <= 0) {
                return;
            }
            BlockPos targetPos = worldPosition.relative(dir);
            net.minecraft.world.level.block.entity.BlockEntity targetBe = level.getBlockEntity(targetPos);
            if (targetBe == null) {
                continue;
            }
            IItemHandler target = targetBe.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (target == null) {
                continue;
            }
            int move = Math.min(budget, out.getCount());
            ItemStack probe = out.copy();
            probe.setCount(move);
            ItemStack leftover = ItemHandlerHelper.insertItem(target, probe, true);
            int can = move - leftover.getCount();
            if (can <= 0) {
                continue;
            }
            ItemStack extracted = inv.extractItem(EnergizingOrbLogic.OUTPUT, can, false);
            ItemStack rem = ItemHandlerHelper.insertItem(target, extracted, false);
            if (!rem.isEmpty()) {
                ItemStack back = inv.insertItem(EnergizingOrbLogic.OUTPUT, rem, false);
                if (!back.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(
                            level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), back);
                }
            }
            budget -= can;
            out = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
        }
        // ME storage — AE2 Interface-style insert
        if (budget > 0 && ModList.get().isLoaded("ae2") && node != null && node.isActive()) {
            ItemStack still = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
            if (still.isEmpty()) {
                return;
            }
            try {
                IStorageService storage = node.getGrid().getService(IStorageService.class);
                if (storage == null) {
                    return;
                }
                MEStorage me = storage.getInventory();
                AEItemKey key = AEItemKey.of(still);
                if (key == null) {
                    return;
                }
                int move = Math.min(budget, still.getCount());
                long real = me.insert(key, move, Actionable.MODULATE, null);
                if (real > 0) {
                    inv.extractItem(EnergizingOrbLogic.OUTPUT, (int) real, false);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) {
            return TickRateModulation.SLEEP;
        }
        if (recipe == null) {
            checkRecipe();
        }
        pullFromNetwork(node);
        autoExportTick(node);
        boolean work = containRecipe
                || !inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()
                || autoExportEnabled()
                || (this instanceof AdvancedEnergizingOrbBlockEntity adv && adv.rodsPresent());
        return work ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(Direction.DOWN);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return dir == Direction.DOWN ? AECableType.COVERED : AECableType.NONE;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("bufferFe", bufferFe);
        tag.putLong("recipeEnergy", recipeEnergy);
        tag.putBoolean("containRecipe", containRecipe);
        tag.putBoolean(NBT_EXPORT, autoExport);
        tag.put("orbInv", inv.serializeNBT());
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        bufferFe = tag.getLong("bufferFe");
        recipeEnergy = tag.getLong("recipeEnergy");
        containRecipe = tag.getBoolean("containRecipe");
        autoExport = tag.getBoolean(NBT_EXPORT);
        if (tag.contains("orbInv")) {
            inv.deserializeNBT(tag.getCompound("orbInv"));
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (itemCap == null || !itemCap.isPresent()) {
                itemCap = LazyOptional.of(() -> new RangedWrapper(inv, 0, EnergizingOrbLogic.SLOTS));
            }
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}
