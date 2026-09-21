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
 * ME Energizing Orb.
 * <ul>
 *   <li>No energy cache. Never pulls AE/FE from the ME network.</li>
 *   <li>Rod-fed only for the current recipe (Powah + AP rods via PowahBridge).</li>
 *   <li>Output craft follows AE2 Inscriber: recipe result count only, extract 1 per input.</li>
 *   <li>Auto-export follows AE2 ME Interface: {@link StorageHelper#poweredInsert}.</li>
 *   <li>Grid: all faces connect (COVERED) so cables on any side see the device.</li>
 * </ul>
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

    protected long bufferFe;
    protected long recipeEnergy;
    protected boolean containRecipe;
    protected boolean autoExport;
    @Nullable
    protected EnergizingRecipe recipe;
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private final MachineSource machineSource = new MachineSource(this);

    public MeEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode()
                .setIdlePowerUsage(0)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setVisualRepresentation(getItemFromBlockEntity())
                .addService(IGridTickable.class, this);
        try {
            autoExport = APConfig.COMMON.orbAutoExport.get();
        } catch (Throwable t) {
            autoExport = false;
        }
    }

    /** Network tool / ME controller name — shared BE types must not show Air. */
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

    @Override
    public boolean containRecipe() {
        return containRecipe;
    }

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
     * Inscriber pattern (InscriberBlockEntity.tickingRequest):
     * <ol>
     *   <li>{@code result = recipe.getResultItem().copy()} — count comes from the recipe JSON only.</li>
     *   <li>Insert into output; abort if it does not fit (do not eat inputs).</li>
     *   <li>Extract <b>exactly 1</b> from each occupied input slot.</li>
     *   <li>Consume {@code recipeEnergy} from the buffer (Advanced keeps leftover cache).</li>
     * </ol>
     * Never multiplies the result. Output may stack the same item up to 64.
     */
    protected void completeIfPossible() {
        if (level == null || recipe == null || recipeEnergy <= 0 || bufferFe < recipeEnergy) {
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
        // Recipe JSON count only (1, 16, …). Reject absurd values instead of inventing stacks.
        int resultCount = result.getCount();
        if (resultCount <= 0) {
            return;
        }
        if (resultCount > 64) {
            AppliedPowahLog(resultCount);
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
                return; // full — wait, do not craft
            }
            after = slot.copy();
            after.grow(resultCount);
        }

        // Commit output, then extract 1 per input (Inscriber).
        inv.setStackInSlot(EnergizingOrbLogic.OUTPUT, after);
        for (int i = 1; i < EnergizingOrbLogic.SLOTS && i < inv.getSlots(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                inv.extractItem(i, 1, false);
            }
        }
        bufferFe = Math.max(0, bufferFe - recipeEnergy);
        recipeEnergy = 0;
        recipe = null;
        containRecipe = false;
        setChanged();
        markForUpdate();
    }

    private static void AppliedPowahLog(int resultCount) {
        com.coala.appliedpowah.AppliedPowah.LOG.warn(
                "Energizing orb refused craft: recipe result count {} > 64", resultCount);
    }

    protected void checkRecipe() {
        if (level == null || level.isClientSide) {
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
        setChanged();
        markForUpdate();
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
     * Auto-export products.
     * <ol>
     *   <li>Inscriber-style push to adjacent inventories.</li>
     *   <li>ME Interface-style {@code StorageHelper.poweredInsert} into network storage.</li>
     * </ol>
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

        // 1) Adjacent inventories — Inscriber.pushOutResult
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

        // 2) ME network — InterfaceLogic.tryUsePlan: StorageHelper.poweredInsert
        if (budget > 0 && ModList.get().isLoaded("ae2") && node != null && node.isActive()) {
            ItemStack still = inv.getStackInSlot(EnergizingOrbLogic.OUTPUT);
            if (still.isEmpty()) {
                return;
            }
            try {
                IStorageService storageService = node.getGrid().getService(IStorageService.class);
                IEnergyService energyService = node.getGrid().getEnergyService();
                if (storageService == null || energyService == null) {
                    return;
                }
                MEStorage networkInv = storageService.getInventory();
                AEItemKey key = AEItemKey.of(still);
                if (key == null) {
                    return;
                }
                int move = Math.min(budget, still.getCount());
                long inserted = StorageHelper.poweredInsert(
                        energyService, networkInv, key, move, machineSource);
                if (inserted > 0) {
                    inv.extractItem(EnergizingOrbLogic.OUTPUT, (int) inserted, false);
                }
            } catch (Throwable t) {
                com.coala.appliedpowah.AppliedPowah.LOG.debug("Orb ME export failed: {}", t.toString());
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
        boolean exported = pushOutToMe(node);
        autoExportTick(node);
        boolean work = containRecipe
                || !inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()
                || autoExportEnabled()
                || (this instanceof AdvancedEnergizingOrbBlockEntity adv && adv.rodsPresent());
        return work || exported ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    /** @return true if anything was pushed into ME this tick. */
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

    /** All faces connect — cables on any side see this machine in the network. */
    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public void onReady() {
        super.onReady();
        getMainNode().setExposedOnSides(EnumSet.allOf(Direction.class));
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
