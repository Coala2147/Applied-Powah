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
import appeng.api.storage.StorageHelper;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.helpers.MachineSource;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
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
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        patterns.writeToNBT(tag);
        selfCraft.save(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        patterns.readFromNBT(tag);
        selfCraft.load(tag);
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

    /** {@link MeInputRouter} 事务式投料。 */
    public boolean routePatternInputs(KeyCounter[] inputHolder) {
        return MeInputRouter.route(inputHolder, craftPorts());
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

    @Override
    protected boolean autoExportEnabled() {
        return false;
    }

    @Override
    protected void autoExportTick(IGridNode node) {
    }

    @Override
    protected boolean pushOutToMe(IGridNode node) {
        return false;
    }

    @Override
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
        int count = result.getCount();
        if (count <= 0 || count > 64) {
            return;
        }

        completing = true;
        try {
            for (int i = 1; i < EnergizingOrbLogic.SLOTS && i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    inv.extractItem(i, 1, false);
                }
            }
            bufferFe = Math.max(0, bufferFe - recipeEnergy);
            recipeEnergy = 0;
            recipe = null;
            containRecipe = false;
            emit(result, count);
        } finally {
            completing = false;
        }
        setChanged();
        markForUpdate();
    }

    private void emit(ItemStack result, int count) {
        try {
            AEItemKey key = AEItemKey.of(result);
            var ret = patterns.getReturnInv();
            if (key != null && ret != null) {
                long inserted = ret.insert(key, count, Actionable.MODULATE, actionSource);
                if (inserted >= count) {
                    return;
                }
                if (inserted > 0) {
                    result.shrink((int) inserted);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            var grid = getMainNode().getGrid();
            if (grid != null) {
                var storage = grid.getService(IStorageService.class);
                var energy = grid.getEnergyService();
                AEItemKey key = AEItemKey.of(result);
                if (storage != null && energy != null && key != null) {
                    long inserted = StorageHelper.poweredInsert(
                            energy, storage.getInventory(), key, result.getCount(), actionSource);
                    if (inserted > 0) {
                        result.shrink((int) inserted);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (!result.isEmpty()) {
            inv.setStackInSlot(EnergizingOrbLogic.OUTPUT, result);
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

    private void drainReturn(IGridNode node) {
        try {
            var ret = patterns.getReturnInv();
            if (ret == null || ret.isEmpty() || node == null || !node.isActive()) {
                return;
            }
            var storage = node.getGrid().getService(IStorageService.class);
            if (storage != null) {
                ret.injectIntoNetwork(storage.getInventory(), actionSource, s -> {
                });
            }
        } catch (Throwable ignored) {
        }
    }

    /** 样板内部自合成 — {@link MeSelfCraftSettings#submitAvailable}。 */
    private boolean runSelfCraft(IGridNode node) {
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
        } catch (Throwable ignored) {
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
}
