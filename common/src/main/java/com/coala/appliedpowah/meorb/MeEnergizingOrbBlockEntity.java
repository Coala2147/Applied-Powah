package com.coala.appliedpowah.meorb;

import appeng.api.config.Actionable;
import appeng.api.config.AccessRestriction;
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
import com.coala.appliedpowah.orb.CraftThread;
import com.coala.appliedpowah.orb.EnergizingOrbLogic;
import com.coala.appliedpowah.orb.EjectMode;
import com.coala.appliedpowah.orb.OrbEjectRules;
import com.coala.appliedpowah.orb.OrbResultRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * ME 充能台 / ME Energizing Orb — 高级台机体 + 4 页多线程 + 内置样板供应器。
 * 自合成引擎照搬 Mek-Energistics（{@link MeSelfCraftSettings} / {@link MeInputRouter}，
 * 命名空间 {@code com.coala.appliedpowah.meorb}）。
 *
 * <p>Grid connectivity is BOTTOM-only (inherited) so an adjacent pattern provider
 * no longer sees a grid connection toward us and pushes its plans normally
 * (issues 0.6.0-7).</p>
 */
public class MeEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity
        implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;

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
            output.putInt("apEjectMode", ejectMode.ordinal());
            output.putInt("apEjectSides", allowedSidesMask());
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            patterns.importSettings(input, player);
            if (input.contains("apEjectMode")) {
                int ordinal = input.getInt("apEjectMode");
                EjectMode[] values = EjectMode.values();
                setEjectMode(ordinal >= 0 && ordinal < values.length ? values[ordinal] : EjectMode.OFF);
            }
            if (input.contains("apEjectSides")) {
                int mask = input.getInt("apEjectSides");
                allowedOutputs.clear();
                for (Direction dir : Direction.values()) {
                    if ((mask & (1 << dir.ordinal())) != 0) {
                        allowedOutputs.add(dir);
                    }
                }
            }
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
        // page.fromCpuOrder is transient — never persist (sticky true forced returnInv).
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        patterns.readFromNBT(tag);
        selfCraft.load(tag);
        passiveCraftingEnabled = tag.getBoolean("PassiveCraftingEnabled");
        for (CraftThread page : threads) {
            page.fromCpuOrder = false;
            page.providerReturnSide = null;
        }
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
        return containRecipe();
    }

    public boolean hasPendingProduct() {
        if (anyPageHasProduct()) {
            return true;
        }
        var ret = patterns.getReturnInv();
        return ret != null && !ret.isEmpty();
    }

    /** {@link MeInputRouter} 事务式投料（自合成 / 内部路径，不算 CPU 下单）。 */
    public boolean routePatternInputs(KeyCounter[] inputHolder) {
        return MeInputRouter.route(inputHolder, craftPorts());
    }

    /**
     * Pattern push into a free page — 产物走 returnInv 回网给 CPU 结算
     * (only when the eject mode is NETWORK, see {@link OrbEjectRules}).
     */
    public boolean acceptCpuPatternInputs(KeyCounter[] inputHolder) {
        List<AEItemKey> singles = MeInputRouter.expandSingles(inputHolder);
        if (singles == null || singles.isEmpty() || singles.size() > CraftThread.INPUTS) {
            return false;
        }
        CraftThread page = findPageFor(singles);
        if (page == null || !page.accept(singles)) {
            return false;
        }
        page.fromCpuOrder = true;
        checkRecipe(page);
        return true;
    }

    public boolean isPassiveCraftingEnabled() {
        return passiveCraftingEnabled;
    }

    public void setPassiveCraftingEnabled(boolean enabled) {
        this.passiveCraftingEnabled = enabled;
        setChanged();
        markForUpdate();
    }

    /** One-item input port per page input slot (transactional, full rollback). */
    private List<MeInputRouter.InputPort> craftPorts() {
        List<MeInputRouter.InputPort> ports = new ArrayList<>();
        for (CraftThread page : threads) {
            var inv = page.getInv();
            for (int slot = 0; slot < CraftThread.INPUTS && slot < inv.getSlots(); slot++) {
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
                        boolean saved = settling;
                        settling = true;
                        try {
                            inv.setStackInSlot(s, key.toStack(1));
                        } finally {
                            settling = saved;
                        }
                        return 1;
                    }

                    @Override
                    public void restore() {
                        boolean saved = settling;
                        settling = true;
                        try {
                            inv.setStackInSlot(s, ItemStack.EMPTY);
                        } finally {
                            settling = saved;
                        }
                    }
                });
            }
        }
        return ports;
    }

    /**
     * CPU settlement: returnInv first (AE2 protocol, NETWORK eject mode only),
     * remainder provider / page OUTPUT.
     */
    @Override
    protected boolean canSettleResult(CraftThread page, ItemStack result) {
        if (!OrbEjectRules.allowsCpuReturn(ejectMode, page.fromCpuOrder)) {
            return super.canSettleResult(page, result);
        }
        ItemStack afterCpu = simulateCpuReturn(result);
        if (afterCpu.isEmpty()) {
            return true;
        }
        return super.canSettleResult(page, afterCpu);
    }

    @Override
    protected void settleResult(CraftThread page, ItemStack result) {
        if (!OrbEjectRules.allowsCpuReturn(ejectMode, page.fromCpuOrder)) {
            super.settleResult(page, result);
            return;
        }
        try {
            ItemStack rest = doCpuReturn(result);
            if (!rest.isEmpty()) {
                rest = page.providerReturnSide != null ? doProviderReturn(page, rest) : rest;
            }
            if (!rest.isEmpty() && !tryPlaceResult(page, rest)) {
                AppliedPowah.LOG.error(
                        "ME orb settle lost product {} x{} at {} page {} (cpu return incomplete)",
                        rest.getItem(), rest.getCount(), worldPosition, pageIndex(page));
                dropStack(rest);
            }
        } finally {
            page.providerReturnSide = null;
            page.fromCpuOrder = false;
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
        // canBeAlerted=true: auto-export / returnInv / cache fill must wake us
        return new TickingRequest(1, 20, false, true);
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
     * returnInv → network. AE2 doWork path (injectIntoNetwork).
     * Eject OFF: do not inject (CPU hangs by design). NETWORK: normal AE2 settle.
     */
    private void drainReturn(IGridNode node) {
        if (!OrbEjectRules.allowsNetworkPush(ejectMode)) {
            return;
        }
        try {
            var ret = patterns.getReturnInv();
            if (ret == null || ret.isEmpty() || node == null || !node.isActive()) {
                return;
            }
            var storage = node.getGrid().getService(IStorageService.class);
            if (storage == null) {
                return;
            }
            // Empty callback matches normal settle; CPU link sees items via storage insert.
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
        if (containRecipe() || hasPendingProduct() || settling) {
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

    // ITEM_HANDLER: inherit base CombinedItemHandler — every face can access all
    // page craft slots (开发总报告 §1.2 / Q16). Do not empty this capability.

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }

    public void openPatternMenu(Player player, MenuLocator locator) {
        appeng.menu.MenuOpener.open(MePatternMenu.TYPE, player, locator);
    }

    /** Icon shown in the ME terminal for this pattern provider. */
    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(APOrbs.ME_ORB_ITEM.get());
    }

    /** Reopen the ME orb GUI (from the pattern page back button). */
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
                return new com.coala.appliedpowah.orb.EnergizingOrbMenu(
                        APOrbs.ME_ORB_MENU.get(), id, inv, MeEnergizingOrbBlockEntity.this);
            }
        }, buf -> buf.writeBlockPos(worldPosition));
    }
}
