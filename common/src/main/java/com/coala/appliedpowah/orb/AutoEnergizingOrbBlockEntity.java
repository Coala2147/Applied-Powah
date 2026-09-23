package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Auto Energizing Orb.
 * Advanced orb + 36 pattern slots (4x9) acting as a Pattern Provider.
 * Pattern inputs are routed directly into the orb's own input slots instead of adjacent blocks.
 */
public class AutoEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;

    private final PatternProviderLogic logic = new AutoOrbPatternProviderLogic(getMainNode(), this, PATTERN_SLOTS);

    public AutoEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // PatternProviderLogic ctor overwrites IGridTickable + sets REQUIRE_CHANNEL.
        // Reclaim the tick slot so the parent orb logic (pull / craft / export) still runs,
        // and drop the channel requirement (orb is a plain ME device).
        getMainNode().setFlags();
        getMainNode().addService(IGridTickable.class, this);
    }

    @Override
    public void onMainNodeStateChanged(appeng.api.networking.IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        // PatternProviderLogic.alertDevice throws if this node is not alertable
        // (we reclaimed IGridTickable from its Ticker). Swallow — our own
        // tickingRequest already drains the return inventory.
        try {
            this.logic.onMainNodeStateChanged();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.logic.updatePatterns();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void saveChanges() {
        setChanged();
        markForUpdate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.logic.writeToNBT(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        this.logic.readFromNBT(tag);
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        this.logic.clearContent();
    }

    @Override
    public int getPriority() {
        return logic.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        logic.setPriority(priority);
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public appeng.api.stacks.AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(APOrbs.AUTO_ORB_ITEM.get()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(APOrbs.AUTO_ORB_ITEM.get());
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

    /** Product not yet returned to the network (output slot or return inv). */
    public boolean hasPendingOutput() {
        if (!inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()) {
            return true;
        }
        var ret = logic == null ? null : logic.getReturnInv();
        return ret != null && !ret.isEmpty();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        var mod = super.tickingRequest(node, ticksSinceLastCall);
        // Pattern-provider return inventory → network (crafting CPU pickup).
        try {
            var ret = logic.getReturnInv();
            if (ret != null && !ret.isEmpty() && node != null && node.isActive()) {
                var storage = node.getGrid().getStorageService();
                if (storage != null) {
                    ret.injectIntoNetwork(storage.getInventory(),
                            new appeng.me.helpers.MachineSource(this),
                            s -> { });
                }
            }
        } catch (Throwable ignored) {
        }
        return mod;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        // Auto orb is fed ONLY by CPU pushPattern → acceptPatternInputs (direct inv writes).
        // Do NOT expose ITEM_HANDLER: adjacent ME interfaces / pipes would stuff input
        // slots and cause runaway overproduction (1 ordered → 2 / 10 → 56).
        // GUI reads orb.getInv() directly (EnergizingOrbMenu), so the screen still works.
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.empty();
        }
        // Do NOT forward pattern-provider GenericStackInv capabilities either
        // (fluid tank / fake energy buffer). PatternProviderMenu reaches those
        // through logic.getPatternInv()/getReturnInv() directly.
        return super.getCapability(cap, side);
    }

    /** Single product path: never auto-export OUTPUT to adjacent inventories. */
    @Override
    protected boolean autoExportEnabled() {
        return false;
    }

    @Override
    protected void autoExportTick(IGridNode node) {
        // no-op — products only leave via returnInv → network
    }

    /** Disable OUTPUT→ME push so returnInv is the only product path (no double count). */
    @Override
    protected boolean pushOutToMe(IGridNode node) {
        return false;
    }

    public void openPatternMenu(Player player, MenuLocator locator) {
        appeng.menu.MenuOpener.open(AutoOrbPatternMenu.TYPE, player, locator);
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }

    @Override
    protected void completeIfPossible() {
        // Single output path: result goes ONLY to the pattern-provider return
        // inventory (CPU pickup). Never leave it in the orb output slot —
        // auto-export would then push a second copy into the network.
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
            for (int i = 1; i < EnergizingOrbLogic.SLOTS && i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    inv.extractItem(i, 1, false);
                }
            }
            bufferFe = Math.max(0, bufferFe - recipeEnergy);
            recipeEnergy = 0;
            recipe = null;
            containRecipe = false;

            // Hand the exact result to the return inventory — once.
            boolean placed = false;
            try {
                AEItemKey key = AEItemKey.of(result);
                if (key != null) {
                    var ret = logic.getReturnInv();
                    if (ret != null) {
                        long inserted = ret.insert(key, resultCount, appeng.api.config.Actionable.MODULATE,
                                new appeng.me.helpers.MachineSource(this));
                        placed = inserted >= resultCount;
                        if (inserted > 0 && inserted < resultCount) {
                            result.shrink((int) inserted);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            if (!placed && !result.isEmpty()) {
                // Fallback: poweredInsert straight into the ME network; only then OUTPUT slot.
                try {
                    var grid = getMainNode().getGrid();
                    if (grid != null) {
                        var storage = grid.getService(IStorageService.class);
                        var energy = grid.getEnergyService();
                        AEItemKey key = AEItemKey.of(result);
                        if (storage != null && key != null) {
                            long inserted = StorageHelper.poweredInsert(energy, storage.getInventory(),
                                    key, result.getCount(), machineSource);
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
        } finally {
            completing = false;
        }
        setChanged();
        markForUpdate();
    }

    /**
     * Accept pattern inputs directly into the orb's input slots (slots 1-6).
     * Transactional: simulate the full batch first, then commit — any failure
     * leaves the orb untouched (never partially stuffed).
     */
    public boolean acceptPatternInputs(KeyCounter[] inputHolder) {
        if (inputHolder == null || inputHolder.length == 0) {
            return false;
        }

        // Flatten to singles — one stack of count 1 per item (2×diamond → 2 slots).
        List<net.minecraft.world.item.ItemStack> singles = new ArrayList<>();
        for (KeyCounter counter : inputHolder) {
            if (counter == null || counter.isEmpty()) {
                continue;
            }
            for (var entry : counter) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (!(key instanceof AEItemKey itemKey) || amount <= 0) {
                    return false;
                }
                for (long n = 0; n < amount; n++) {
                    singles.add(itemKey.toStack(1));
                }
            }
        }
        if (singles.isEmpty()) {
            return false;
        }

        // Plan: map each single into an empty input slot; abort whole batch if not possible.
        int[] planSlot = new int[singles.size()];
        int idx = 0;
        for (int slot = 1; slot < inv.getSlots() && slot < EnergizingOrbLogic.SLOTS && idx < singles.size(); slot++) {
            if (inv.getStackInSlot(slot).isEmpty()
                    && EnergizingOrbLogic.canInsertInput(inv, slot, singles.get(idx))) {
                planSlot[idx] = slot;
                idx++;
            }
        }
        if (idx != singles.size()) {
            return false;
        }

        // Commit under completing guard so onContentsChanged does not storm checkRecipe.
        boolean completingSaved = completing;
        completing = true;
        try {
            for (int i = 0; i < singles.size(); i++) {
                inv.setStackInSlot(planSlot[i], singles.get(i));
            }
        } finally {
            completing = completingSaved;
        }
        checkRecipe();
        return true;
    }
}
