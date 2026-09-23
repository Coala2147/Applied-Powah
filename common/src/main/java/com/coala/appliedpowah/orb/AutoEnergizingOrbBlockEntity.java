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
 * <p>Full rewrite (0.4.3): body = {@link AdvancedEnergizingOrbBlockEntity};
 * pattern shell + <b>passive self-crafting</b> ported from
 * Mek-Energistics {@code MePassiveCraftingDispatcher}
 * (refs/1.21.1/Mek-Energistics-main). Pattern page assets = ExtendedAE 1.21.1
 * {@code ex_pattern_provider}.</p>
 *
 * <p>Crafting paths:</p>
 * <ol>
 *   <li><b>Passive (样板内部自合成)</b> — every interval, pull ingredients for one
 *       of the held patterns out of the ME network and feed this orb.</li>
 *   <li><b>CPU pushPattern</b> — crafting CPU can still dispatch to this provider.</li>
 * </ol>
 * Products leave only through {@code returnInv → network} (Mek single-exit).
 */
public class AutoEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity
        implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;

    private final PatternProviderLogic logic =
            new AutoOrbPatternProviderLogic(getMainNode(), this, PATTERN_SLOTS);
    private final MachineSource machineSource = new MachineSource(this);
    private final MePassiveCraft passiveCraft = MePassiveCraft.create();

    public AutoEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // PatternProviderLogic steals IGridTickable + sets REQUIRE_CHANNEL.
        // Keep our ticker (advanced pull / complete / passive craft) and drop the flag.
        getMainNode().setFlags();
        getMainNode().addService(IGridTickable.class, this);
    }

    // ------------------------------------------------------------------
    // PatternProviderLogicHost
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Lifecycle / NBT
    // ------------------------------------------------------------------

    @Override
    public void onReady() {
        super.onReady();
        logic.updatePatterns();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        try {
            logic.onMainNodeStateChanged();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        logic.writeToNBT(tag);
        passiveCraft.save(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        logic.readFromNBT(tag);
        passiveCraft.load(tag);
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

    // ------------------------------------------------------------------
    // Busy / pending
    // ------------------------------------------------------------------

    public boolean hasPendingOutput() {
        if (!inv.getStackInSlot(EnergizingOrbLogic.OUTPUT).isEmpty()) {
            return true;
        }
        var ret = logic.getReturnInv();
        return ret != null && !ret.isEmpty();
    }

    // ------------------------------------------------------------------
    // Transactional input intake (Mek MePatternInputRouter.executePlan)
    // ------------------------------------------------------------------

    /**
     * Accept pattern inputs into orb input slots 1-6 (one item per slot).
     * Plan first, commit only if the whole batch fits — never partial.
     */
    public boolean acceptPatternInputs(KeyCounter[] inputHolder) {
        List<ItemStack> singles = flattenSingles(inputHolder);
        if (singles == null || singles.isEmpty()) {
            return false;
        }

        int[] planSlot = new int[singles.size()];
        int planned = 0;
        for (int slot = 1; slot < EnergizingOrbLogic.SLOTS && slot < inv.getSlots() && planned < singles.size(); slot++) {
            ItemStack probe = singles.get(planned);
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
                inv.setStackInSlot(planSlot[i], singles.get(i));
            }
        } finally {
            completing = saved;
        }
        checkRecipe();
        return true;
    }

    private static List<ItemStack> flattenSingles(KeyCounter[] inputHolder) {
        if (inputHolder == null) {
            return null;
        }
        List<ItemStack> singles = new ArrayList<>();
        for (KeyCounter counter : inputHolder) {
            if (counter == null || counter.isEmpty()) {
                continue;
            }
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return null;
                }
                for (long n = 0; n < entry.getLongValue(); n++) {
                    singles.add(itemKey.toStack(1));
                }
            }
        }
        return singles;
    }

    // ------------------------------------------------------------------
    // Product path — SINGLE exit
    // ------------------------------------------------------------------

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
            emitProduct(result, resultCount);
        } finally {
            completing = false;
        }
        setChanged();
        markForUpdate();
    }

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

    // ------------------------------------------------------------------
    // Tick — parent work + returnInv drain + PASSIVE self-crafting
    // ------------------------------------------------------------------

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        TickRateModulation mod = super.tickingRequest(node, ticksSinceLastCall);
        drainReturnInventory(node);
        boolean crafted = runPassiveCraft(node);
        return crafted ? TickRateModulation.URGENT : mod;
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
     * 样板内部自合成 — Mek MePassiveCraftingDispatcher path.
     * Pull ingredients for held patterns out of the ME network and feed this orb.
     * Skips while a recipe is mid-run or the product has not left.
     */
    private boolean runPassiveCraft(IGridNode node) {
        if (level == null || level.isClientSide || node == null || !node.isActive()) {
            return false;
        }
        if (!passiveCraft.tick()) {
            return false;
        }
        if (containRecipe || hasPendingOutput() || completing) {
            return false;
        }
        try {
            var storage = node.getGrid().getService(IStorageService.class);
            if (storage == null) {
                return false;
            }
            var patterns = logic.getAvailablePatterns();
            return passiveCraft.submitAvailable(patterns, level, storage.getInventory(), machineSource,
                    this::acceptPatternInputs);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Capabilities / menu
    // ------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        // Never expose ITEM_HANDLER — neighbour pipes would stuff inputs (overproduction).
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
