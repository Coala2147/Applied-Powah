package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.MachineSource;
import com.coala.appliedpowah.config.APConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
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
 * ME Energizing Orb — rod-fed recipe machine + GUI + ME auto-export.
 * No independent energy cache. Grid connects on BOTTOM only.
 *
 * Craft completion uses a re-entrancy guard: writing the output must not
 * re-enter completeIfPossible via onContentsChanged → checkRecipe
 * (that path produced 1→64 stacks).
 */
public class MeEnergizingOrbBlockEntity extends AENetworkBlockEntity
        implements IGridTickable, EnergyAcceptingOrb, IAEPowerStorage {

    public static final String NBT_EXPORT = "ap_orb_auto_export";

    protected final ItemStackHandler inv = new ItemStackHandler(EnergizingOrbLogic.SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!completing) {
                checkRecipe();
            }
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == EnergizingOrbLogic.OUTPUT
                    || EnergizingOrbLogic.canInsertInput(this, slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == EnergizingOrbLogic.OUTPUT ? 64 : 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != EnergizingOrbLogic.OUTPUT && !stack.isEmpty()) {
                ItemStack single = stack.copy();
                single.setCount(1);
                if (!super.insertItem(slot, single, true).isEmpty()) {
                    return stack;
                }
                if (simulate) {
                    return shrinkCopy(stack, 1);
                }
                super.insertItem(slot, single, false);
                return shrinkCopy(stack, 1);
            }
            return super.insertItem(slot, stack, simulate);
        }

        private ItemStack shrinkCopy(ItemStack stack, int n) {
            if (stack.getCount() <= n) {
                return ItemStack.EMPTY;
            }
            ItemStack rest = stack.copy();
            rest.shrink(n);
            return rest;
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

    /** Blocks re-entrant completeIfPossible while writing output / extracting inputs. */
    protected boolean completing;
    protected long bufferFe;
    protected long recipeEnergy;
    protected boolean containRecipe;
    protected boolean autoExport;
    /** True when a loaded recipe has no incoming energy (power alert). */
    protected boolean showWarning;
    @Nullable
    protected EnergizingRecipe recipe;
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private final MachineSource machineSource = new MachineSource(this);

    public MeEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode()
                .setIdlePowerUsage(0)
                .setExposedOnSides(EnumSet.of(Direction.DOWN))
                .setVisualRepresentation(getItemFromBlockEntity())
                .addService(IGridTickable.class, this);
        try {
            autoExport = APConfig.COMMON.orbAutoExport.get();
        } catch (Throwable t) {
            autoExport = false;
        }
    }

    @Override
    protected Item getItemFromBlockEntity() {
        try {
            return getBlockState().getBlock().asItem();
        } catch (Exception e) {
            return super.getItemFromBlockEntity();
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

    public boolean isShowWarning() {
        return showWarning;
    }

    @Override
    public boolean containRecipe() {
        return containRecipe;
    }

    @Override
    public long fillEnergy(long amount) {
        if (level == null || recipe == null || recipeEnergy <= 0 || amount <= 0 || completing) {
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
     * Inscriber-style finish:
     * 1) re-entrancy guard
     * 2) result = recipe.getResultItem().copy() only
     * 3) extract 1 per input FIRST (so callbacks cannot re-match)
     * 4) write output
     * 5) consume recipeEnergy
     */
    protected void completeIfPossible() {
        if (completing || level == null || recipe == null || recipeEnergy <= 0 || bufferFe < recipeEnergy) {
            return;
        }
        ItemStack result;
        try {
            result = recipe.getResultItem(level.registryAccess()).copy();
        } catch (Throwable t) {
            try {
                result = recipe.getResultItem().copy();
            } catch (Throwable ignored) {
                return;
            }
        }
        if (result.isEmpty()) {
            return;
        }
        int resultCount = result.getCount();
        if (resultCount <= 0 || resultCount > 64) {
            return;
        }

        ItemStack slot = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
        ItemStack after;
        if (slot.isEmpty()) {
            after = result.copy();
        } else {
            if (!ItemStack.isSameItemSameTags(slot, result)) {
                return;
            }
            if (slot.getCount() + resultCount > 64) {
                return;
            }
            after = slot.copy();
            after.grow(resultCount);
        }

        completing = true;
        try {
            // Consume inputs first — onContentsChanged will see no recipe.
            for (int i = 1; i < EnergizingOrbLogic.SLOTS && i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    inv.extractItem(i, 1, false);
                }
            }
            inv.setStackInSlot(EnergizingOrbLogic.OUTPUT, after);
            bufferFe = Math.max(0, bufferFe - recipeEnergy);
            recipeEnergy = 0;
            recipe = null;
            containRecipe = false;
        } finally {
            completing = false;
        }
        setChanged();
        markForUpdate();
    }

    protected void checkRecipe() {
        if (level == null || level.isClientSide || completing) {
            return;
        }
        EnergizingRecipe found = EnergizingOrbLogic.findRecipe(level, inv);
        if (found != null) {
            if (recipe != found && !(this instanceof AdvancedEnergizingOrbBlockEntity)) {
                bufferFe = 0;
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
        updateWarning();
        setChanged();
        markForUpdate();
    }

    /**
     * reaction_chamber-style power alert.
     * ME: recipe loaded but no rod energy.
     * Advanced: rods present + recipe, cache empty (network/rods not feeding).
     */
    protected void updateWarning() {
        boolean warn = false;
        if (recipe != null && recipeEnergy > 0 && bufferFe <= 0) {
            if (this instanceof AdvancedEnergizingOrbBlockEntity adv) {
                warn = adv.rodsPresent();
            } else {
                warn = true;
            }
        }
        if (warn != showWarning) {
            showWarning = warn;
        }
    }

    public boolean isAeDisplay() {
        return false;
    }

    public long getDisplayEnergy() {
        return recipeEnergy > 0 ? bufferFe : 0;
    }

    public long getDisplayCapacity() {
        return recipeEnergy > 0 ? recipeEnergy : 0;
    }

    public String getEnergyUnit() {
        return isAeDisplay() ? "AE" : "FE";
    }

    // ---- Jade / AE2 IAEPowerStorage — ME orb has no cache (0/0). ----
    // Advanced overrides to report the rod cache (energy-cell style).

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

    /** ME orb: never extract AE/FE from the network. */
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

    protected void autoExportTick(IGridNode node) {
        if (!autoExportEnabled() || level == null || level.isClientSide) {
            return;
        }
        int budget = autoExportBudget();
        ItemStack out = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
        if (out.isEmpty()) {
            return;
        }
        // Adjacent inventories — Inscriber.pushOutResult
        for (Direction dir : Direction.values()) {
            if (budget <= 0) {
                return;
            }
            BlockPos targetPos = worldPosition.relative(dir);
            var targetBe = level.getBlockEntity(targetPos);
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
        if (recipe == null && !completing) {
            checkRecipe();
        }
        pullFromNetwork(node);
        autoExportTick(node);
        boolean exported = pushOutToMe(node);
        updateWarning();
        boolean work = containRecipe
                || !inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()
                || autoExportEnabled()
                || (this instanceof AdvancedEnergizingOrbBlockEntity adv && adv.rodsPresent());
        return work || exported ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    private boolean pushOutToMe(IGridNode node) {
        if (!autoExportEnabled() || node == null || !node.isActive()) {
            return false;
        }
        ItemStack still = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
        if (still.isEmpty() || !ModList.get().isLoaded("ae2")) {
            return false;
        }
        try {
            IStorageService storageService = node.getGrid().getService(IStorageService.class);
            IEnergyService energyService = node.getGrid().getEnergyService();
            if (storageService == null || energyService == null) {
                return false;
            }
            AEItemKey key = AEItemKey.of(still);
            if (key == null) {
                return false;
            }
            long sim = StorageHelper.poweredInsert(
                    energyService, storageService.getInventory(), key, still.getCount(),
                    machineSource, Actionable.SIMULATE);
            if (sim <= 0) {
                return false;
            }
            long real = StorageHelper.poweredInsert(
                    energyService, storageService.getInventory(), key, sim, machineSource);
            if (real > 0) {
                inv.extractItem(EnergizingOrbLogic.OUTPUT, (int) real, false);
                setChanged();
                markForUpdate();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** BOTTOM only — user-tested ground cable connects; do not open other faces. */
    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(Direction.DOWN);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return dir == Direction.DOWN ? AECableType.COVERED : AECableType.NONE;
    }

    @Override
    public void onReady() {
        super.onReady();
        getMainNode().setExposedOnSides(EnumSet.of(Direction.DOWN));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("bufferFe", bufferFe);
        tag.putLong("recipeEnergy", recipeEnergy);
        tag.putBoolean("containRecipe", containRecipe);
        tag.putBoolean(NBT_EXPORT, autoExport);
        tag.putBoolean("showWarning", showWarning);
        tag.put("orbInv", inv.serializeNBT());
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        bufferFe = tag.getLong("bufferFe");
        recipeEnergy = tag.getLong("recipeEnergy");
        containRecipe = tag.getBoolean("containRecipe");
        autoExport = tag.getBoolean(NBT_EXPORT);
        showWarning = tag.getBoolean("showWarning");
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
