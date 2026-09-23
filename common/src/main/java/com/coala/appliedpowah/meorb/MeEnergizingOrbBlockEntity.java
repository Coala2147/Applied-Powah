package com.coala.appliedpowah.meorb;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.helpers.MachineSource;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.orb.APOrbs;
import com.coala.appliedpowah.orb.AdvancedEnergizingOrbBlockEntity;
import com.coala.appliedpowah.orb.EnergizingOrbLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * ME 充能台 / ME Energizing Orb — 机体复用高级充能台，样板页 EAE 1.21.1，
 * 自合成引擎照搬 Mek-Energistics（{@link MeSelfCraftSettings} / {@link MeInputRouter}，
 * 命名空间 {@code com.coala.appliedpowah.meorb}）。
 */
public class MeEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity
        implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;
    public static final int CRAFT_SLOTS = 6;

    private final PatternProviderLogic patterns = new MePatternLogic(getMainNode(), this, PATTERN_SLOTS);
    private final MeSelfCraftSettings selfCraft = new MeSelfCraftSettings();
    private final MachineSource actionSource = new MachineSource(this);
    /**
     * Mek MeAeMachine.hasPassiveCraftingUpgrade — must be installed/on before
     * processPassiveCrafting runs. Default false: no craft without a CPU order.
     */
    private boolean passiveCraftingEnabled;

    public MeEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode().setFlags();
        getMainNode().addService(IGridTickable.class, this);
    }

    @Override
    public PatternProviderLogic getLogic() {
        return patterns;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void saveChanges() {
        setChanged();
        markForUpdate();
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(APOrbs.ME_ORB_ITEM.get()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(APOrbs.ME_ORB_ITEM.get());
    }

    @Override
    public int getPriority() {
        return patterns.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        patterns.setPriority(priority);
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            patterns.exportSettings(output);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            patterns.importSettings(input, player);
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        patterns.updatePatterns();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        try {
            patterns.onMainNodeStateChanged();
        } catch (Exception e) {
            AppliedPowah.LOG.debug("patterns.onMainNodeStateChanged: {}", e.toString());
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        patterns.writeToNBT(tag);
        selfCraft.save(tag);
        tag.putBoolean("PassiveCraftingEnabled", passiveCraftingEnabled);
        // inputFromCpuOrder is transient — never persist (sticky true forced returnInv).
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        patterns.readFromNBT(tag);
        selfCraft.load(tag);
        passiveCraftingEnabled = tag.getBoolean("PassiveCraftingEnabled");
        inputFromCpuOrder = false;
        providerReturnSide = null;
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        patterns.addDrops(drops);
    }

    @Override
    public void clearContent() {
        patterns.clearContent();
    }

    public boolean isCrafting() {
        return containRecipe;
    }

    public boolean hasPendingProduct() {
        if (!inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()) {
            return true;
        }
        var ret = patterns.getReturnInv();
        return ret != null && !ret.isEmpty();
    }

    /** {@link MeInputRouter} 事务式投料（自合成 / 内部路径，不算 CPU 下单）。 */
    public boolean routePatternInputs(KeyCounter[] inputHolder) {
        inputFromCpuOrder = false;
        return MeInputRouter.route(inputHolder, craftPorts());
    }

    /**
     * CPU pushPattern 下单投料 — 产物走 returnInv 回网给 CPU 结算。
     * 标志仅在 pushPattern→完成 之间存活，不进 NBT。
     */
    public boolean acceptCpuPatternInputs(KeyCounter[] inputHolder) {
        boolean ok = MeInputRouter.route(inputHolder, craftPorts());
        inputFromCpuOrder = ok;
        return ok;
    }

    public boolean isPassiveCraftingEnabled() {
        return passiveCraftingEnabled;
    }

    public void setPassiveCraftingEnabled(boolean enabled) {
        this.passiveCraftingEnabled = enabled;
        setChanged();
        markForUpdate();
    }

    private List<MeInputRouter.InputPort> craftPorts() {
        List<MeInputRouter.InputPort> ports = new ArrayList<>(CRAFT_SLOTS);
        for (int slot = 1; slot <= CRAFT_SLOTS && slot < inv.getSlots(); slot++) {
            final int s = slot;
            ports.add(new MeInputRouter.InputPort() {
                @Override
                public boolean empty() {
                    return inv.getStackInSlot(s).isEmpty();
                }

                @Override
                public boolean supports(AEItemKey key) {
                    return EnergizingOrbLogic.canInsertInput(inv, s, key.toStack(1));
                }

                @Override
                public long insert(AEItemKey key, long amount, boolean simulate) {
                    if (simulate) {
                        return empty() && supports(key) ? 1 : 0;
                    }
                    if (!empty() || !supports(key)) {
                        return 0;
                    }
                    boolean saved = completing;
                    completing = true;
                    try {
                        inv.setStackInSlot(s, key.toStack(1));
                    } finally {
                        completing = saved;
                    }
                    return 1;
                }

                @Override
                public void restore() {
                    boolean saved = completing;
                    completing = true;
                    try {
                        inv.setStackInSlot(s, ItemStack.EMPTY);
                    } finally {
                        completing = saved;
                    }
                }
            });
        }
        return ports;
    }

    /**
     * CPU settlement: returnInv first (AE2 PatternProvider protocol), remainder
     * via provider return / OUTPUT. Never overwrite OUTPUT.
     * Empty leftover after returnInv means success.
     */
    @Override
    protected boolean canSettleResult(ItemStack result) {
        if (!inputFromCpuOrder) {
            return super.canSettleResult(result);
        }
        ItemStack afterCpu = simulateCpuReturn(result);
        if (afterCpu.isEmpty()) {
            return true;
        }
        return super.canSettleResult(afterCpu);
    }

    @Override
    protected void settleResult(ItemStack result) {
        if (!inputFromCpuOrder) {
            super.settleResult(result);
            return;
        }
        inputFromCpuOrder = false;
        try {
            ItemStack rest = doCpuReturn(result);
            if (!rest.isEmpty()) {
                rest = providerReturnSide != null ? doProviderReturn(rest) : rest;
            }
            if (!rest.isEmpty() && !tryPlaceResult(rest)) {
                AppliedPowah.LOG.error(
                        "ME orb settle lost product {} x{} at {} (cpu return incomplete)",
                        rest.getItem(), rest.getCount(), worldPosition);
                net.minecraft.world.Containers.dropItemStack(
                        level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), rest);
            }
        } finally {
            providerReturnSide = null;
        }
    }

    private ItemStack simulateCpuReturn(ItemStack result) {
        try {
            AEItemKey key = AEItemKey.of(result);
            var ret = patterns.getReturnInv();
            if (key == null || ret == null) {
                return result;
            }
            long inserted = ret.insert(key, result.getCount(), Actionable.SIMULATE, actionSource);
            if (inserted >= result.getCount()) {
                return ItemStack.EMPTY;
            }
            ItemStack left = result.copy();
            left.shrink((int) inserted);
            return left;
        } catch (Exception e) {
            AppliedPowah.LOG.debug("simulateCpuReturn failed: {}", e.toString());
            return result;
        }
    }

    private ItemStack doCpuReturn(ItemStack result) {
        try {
            AEItemKey key = AEItemKey.of(result);
            var ret = patterns.getReturnInv();
            if (key == null || ret == null) {
                return result;
            }
            long inserted = ret.insert(key, result.getCount(), Actionable.MODULATE, actionSource);
            if (inserted >= result.getCount()) {
                return ItemStack.EMPTY;
            }
            if (inserted > 0) {
                result.shrink((int) inserted);
            }
            return result;
        } catch (Exception e) {
            AppliedPowah.LOG.warn("CPU returnInv insert failed at {}: {}", worldPosition, e.toString());
            return result;
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        TickRateModulation mod = super.tickingRequest(node, ticksSinceLastCall);
        drainReturn(node);
        if (runSelfCraft(node)) {
            return TickRateModulation.URGENT;
        }
        return mod;
    }

    /**
     * returnInv → network: CPU-order settlement only (AE2 PatternProviderLogic.doWork).
     * This is the single injectIntoNetwork path — PatternProviderLogic's Ticker is
     * intentionally shadowed by this BE's IGridTickable service. Not gated by autoExport.
     */
    private void drainReturn(IGridNode node) {
        try {
            var ret = patterns.getReturnInv();
            if (ret == null || ret.isEmpty() || node == null || !node.isActive()) {
                return;
            }
            var storage = node.getGrid().getService(IStorageService.class);
            if (storage == null) {
                return;
            }
            ret.injectIntoNetwork(storage.getInventory(), actionSource, s -> {
            });
        } catch (Exception e) {
            AppliedPowah.LOG.warn("drainReturn failed at {}: {}", worldPosition, e.toString());
        }
    }

    /**
     * 样板内部自合成 — 对齐 Mek {@code processPassiveCrafting(enabled)}：
     * 必须先启用（默认 false，等价未装 PASSIVE_CRAFTING 升级）。
     * 无下单、未启用时绝不从网络抽料合成。
     */
    private boolean runSelfCraft(IGridNode node) {
        if (!passiveCraftingEnabled) {
            return false;
        }
        if (level == null || level.isClientSide || node == null || !node.isActive()) {
            return false;
        }
        if (!selfCraft.tick()) {
            return false;
        }
        if (containRecipe || hasPendingProduct() || completing) {
            return false;
        }
        try {
            var storage = node.getGrid().getService(IStorageService.class);
            if (storage == null) {
                return false;
            }
            return selfCraft.submitAvailable(
                    patterns.getAvailablePatterns(),
                    selfCraft.multiplier(),
                    level,
                    storage.getInventory(),
                    actionSource,
                    this::routePatternInputs);
        } catch (Exception e) {
            AppliedPowah.LOG.warn("runSelfCraft failed at {}: {}", worldPosition, e.toString());
            return false;
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }

    public void openPatternMenu(Player player, MenuLocator locator) {
        appeng.menu.MenuOpener.open(MePatternMenu.TYPE, player, locator);
    }

    /** 从样板页返回 ME 台 GUI（与右键台打开同一菜单）。 */
    public void openOrbMenu(net.minecraft.server.level.ServerPlayer player) {
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return getBlockState().getBlock().getName();
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv,
                    net.minecraft.world.entity.player.Player p) {
                return new com.coala.appliedpowah.orb.EnergizingOrbMenu(id, inv, MeEnergizingOrbBlockEntity.this);
            }
        }, buf -> buf.writeBlockPos(worldPosition));
    }
}
