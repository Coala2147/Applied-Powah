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
 * ME Energizing Orb — MUST be rod-fed; never pulls AE/FE from the ME network.
 * Grid connection: BOTTOM only. Auto-export is rate-limited (ExInscriber-style).
 * {@link IAEPowerStorage} is display-only so AE2 Jade shows stored energy like a cell/controller.
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
    };

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
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
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
        if (level == null || recipe == null || recipeEnergy <= 0) {
            return 0;
        }
        long empty = Math.max(0, recipeEnergy - bufferFe);
        long filled = amount <= 0 ? 0 : Math.min(empty, amount);
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

    protected void completeIfPossible() {
        if (level == null || recipe == null || recipeEnergy <= 0 || bufferFe < recipeEnergy) {
            return;
        }
        // 产物槽必须为空才完成一次配方；禁止堆叠/一次输入多份输出。
        ItemStack out = EnergizingOrbLogic.resultOf(level, recipe);
        if (!out.isEmpty() && inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()) {
            EnergizingOrbLogic.clearInputs(inv);
            inv.setStackInSlot(EnergizingOrbLogic.OUTPUT, out);
            bufferFe = 0;
            recipeEnergy = 0;
            recipe = null;
            containRecipe = false;
            setChanged();
            markForUpdate();
        }
    }

    protected void checkRecipe() {
        if (level == null || level.isClientSide) {
            return;
        }
        EnergizingRecipe found = EnergizingOrbLogic.findRecipe(level, inv);
        if (found != null) {
            if (recipe != found) {
                bufferFe = 0;
            }
            recipe = found;
            recipeEnergy = found.getEnergy();
            containRecipe = true;
            // 产物被取走后，若缓存已足够则立刻完成一次（仍要求输出槽为空）
            if (bufferFe >= recipeEnergy) {
                completeIfPossible();
            }
        } else {
            recipe = null;
            recipeEnergy = 0;
            bufferFe = 0;
            containRecipe = false;
        }
        setChanged();
        markForUpdate();
    }

    /** AE2 Jade 复用：IAEPowerStorage 展示用当前值（AE 家族 FE/2，ME 显示 FE 数值）。 */
    public boolean isAeDisplay() {
        return false;
    }

    public long getDisplayEnergy() {
        return bufferFe;
    }

    public long getDisplayCapacity() {
        if (recipeEnergy > 0) {
            return recipeEnergy;
        }
        return 0;
    }

    public String getEnergyUnit() {
        return isAeDisplay() ? "AE" : "FE";
    }

    // ---- AE2 IAEPowerStorage — reuse AE2 Jade; no custom plugin ----

    @Override
    public double getAECurrentPower() {
        return getDisplayEnergy();
    }

    @Override
    public double getAEMaxPower() {
        // 0 → AE2 Jade omits the Stored line (idle orb / no rods / no recipe).
        return getDisplayCapacity();
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
        try {
            return autoExport || APConfig.COMMON.orbAutoExport.get();
        } catch (Throwable t) {
            return autoExport;
        }
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
        // Adjacent inventories — ExInscriber pushOutResult pattern
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
        // ME storage — rate-limited insert (needs an active node for grid access)
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
                || autoExportEnabled();
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
