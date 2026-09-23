package com.coala.appliedpowah.orb;

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
 * ME Energizing Orb — registry id {@code me_energizing_orb}.
 *
 * <p>Body = {@link AdvancedEnergizingOrbBlockEntity} (4 rod slots, FE cache,
 * network pull, Powah energizing recipes, shared GUI).</p>
 *
 * <p>Shell = AE2 {@link PatternProviderLogic} with 36 pattern slots (4×9,
 * ExtendedAE page). CPU {@code pushPattern} feeds the orb's own input slots;
 * products leave through a single path ({@code returnInv} → network).</p>
 *
 * <p>Three Mek-Energistics factory disciplines (see
 * {@code docs/feat/调研_ME充能台_熔炼工厂架构与ExtendedAE样板页.md}):</p>
 * <ol>
 *   <li>Input routing is transactional — plan, then commit; never partial.</li>
 *   <li>{@code isBusy()} is the CPU's only throttle and is truthful.</li>
 *   <li>One product exit only — no auto-export double-count.</li>
 * </ol>
 */
public class AutoEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity
        implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;

    private final PatternProviderLogic logic =
            new AutoOrbPatternProviderLogic(getMainNode(), this, PATTERN_SLOTS);
    private final MachineSource machineSource = new MachineSource(this);

    public AutoEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // PatternProviderLogic ctor steals IGridTickable and sets REQUIRE_CHANNEL.
        // Take the ticker back (advanced-orb pull/complete still runs on it) and
        // drop the channel flag — this orb is a plain ME device.
        getMainNode().setFlags();
        getMainNode().addService(IGridTickable.class, this);
    }

    // ---------------------------------------------------------------------
    // PatternProviderLogicHost
    // ---------------------------------------------------------------------

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
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

    /** No side targets: original pushPattern has nowhere to send — only our path. */
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
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            logic.exportSettings(output);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            logic.importSettings(input, player);
        }
    }

    @Override
    public int getPriority() {
        return logic.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        logic.setPriority(priority);
    }

    // ---------------------------------------------------------------------
    // Lifecycle / NBT
    // ---------------------------------------------------------------------

    @Override
    public void onReady() {
        super.onReady();
        logic.updatePatterns();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        // logic.alertDevice throws when this node is not alertable (we hold the ticker).
        try {
            logic.onMainNodeStateChanged();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        logic.writeToNBT(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        logic.readFromNBT(tag);
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        logic.clearContent();
    }

    // ---------------------------------------------------------------------
    // CPU handshake
    // ---------------------------------------------------------------------

    /** True while a recipe is loaded or the product has not left the orb. */
    public boolean hasPendingOutput() {
        if (!inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()) {
            return true;
        }
        var ret = logic.getReturnInv();
        return ret != null && !ret.isEmpty();
    }

    /**
     * Transactional pattern-input intake (Mek discipline 1).
     * Plan every single into an empty input slot first; commit only if the whole
     * batch fits. Never leave half a recipe in the machine.
     */
    public boolean acceptPatternInputs(KeyCounter[] inputHolder) {
        var singles = AutoOrbPatternProviderLogic.flatten(inputHolder);
        if (singles == null || singles.isEmpty()) {
            return false;
        }

        int[] planSlot = new int[singles.size()];
        int planned = 0;
        for (int slot = 1; slot < EnergizingOrbLogic.SLOTS && slot < inv.getSlots() && planned < singles.size(); slot++) {
            ItemStack probe = singles.get(planned).key().toStack(1);
            if (inv.getStackInSlot(slot).isEmpty()
                    && EnergizingOrbLogic.canInsertInput(inv, slot, probe)) {
                planSlot[planned] = slot;
                planned++;
            }
        }
        if (planned != singles.size()) {
            return false;
        }

        boolean saved = completing;
        completing = true;
        try {
            for (int i = 0; i < singles.size(); i++) {
                inv.setStackInSlot(planSlot[i], singles.get(i).key().toStack(1));
            }
        } finally {
            completing = saved;
        }
        checkRecipe();
        return true;
    }

    // ---------------------------------------------------------------------
    // Product path — SINGLE exit (Mek discipline 3)
    // ---------------------------------------------------------------------

    /** Adjacent-inventory auto-export is disabled. */
    @Override
    protected boolean autoExportEnabled() {
        return false;
    }

    @Override
    protected void autoExportTick(IGridNode node) {
    }

    /** OUTPUT→ME push is disabled — returnInv is the only exit. */
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
        int resultCount = result.getCount();
        if (resultCount <= 0 || resultCount > 64) {
            return;
        }

        completing = true;
        try {
            // Consume one item from every filled input slot first.
            for (int i = 1; i < EnergizingOrbLogic.SLOTS && i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    inv.extractItem(i, 1, false);
                }
            }
            bufferFe = Math.max(0, bufferFe - recipeEnergy);
            recipeEnergy = 0;
            recipe = null;
            containRecipe = false;

            emitProduct(result, resultCount);
        } finally {
            completing = false;
        }
        setChanged();
        markForUpdate();
    }

    /**
     * Hand the product to the pattern return inventory (CPU pickup).
     * Fallback: poweredInsert into the ME network. Last resort: OUTPUT slot
     * (item is never voided; OUTPUT is not auto-exported).
     */
    private void emitProduct(ItemStack result, int resultCount) {
        try {
            AEItemKey key = AEItemKey.of(result);
            var ret = logic.getReturnInv();
            if (key != null && ret != null) {
                long inserted = ret.insert(key, resultCount, Actionable.MODULATE, machineSource);
                if (inserted >= resultCount) {
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
                            energy, storage.getInventory(), key, result.getCount(), machineSource);
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

    // ---------------------------------------------------------------------
    // Tick / capabilities / menu
    // ---------------------------------------------------------------------

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        TickRateModulation mod = super.tickingRequest(node, ticksSinceLastCall);
        drainReturnInventory(node);
        return mod;
    }

    private void drainReturnInventory(IGridNode node) {
        try {
            var ret = logic.getReturnInv();
            if (ret == null || ret.isEmpty() || node == null || !node.isActive()) {
                return;
            }
            var storage = node.getGrid().getService(IStorageService.class);
            if (storage != null) {
                ret.injectIntoNetwork(storage.getInventory(), machineSource, s -> {
                });
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * ITEM_HANDLER is NOT exposed. CPU feeds via pushPattern → acceptPatternInputs
     * (direct inv writes). Neighbour pipes/interfaces must not stuff input slots —
     * that was the runaway overproduction vector. GUI reads {@code orb.getInv()}.
     */
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
        appeng.menu.MenuOpener.open(AutoOrbPatternMenu.TYPE, player, locator);
    }
}
