package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import appeng.me.helpers.MachineSource;
import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.config.APConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedItemHandler;
import owmii.powah.block.energizing.EnergizingRecipe;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Energizing Orb base — multi-page (thread) recipe machine.
 *
 * <ul>
 *   <li>Fluix tier = 1 page; Advanced / ME tiers = 4 pages
 *       ({@link #pageCount()}, ExtendedAE ex-molecular-assembler style).</li>
 *   <li>Every page: 6 inputs + 1 output, independent recipe + progress,
 *       one completion = one full recipe set (never a multiplier).</li>
 *   <li>5-slot AE2 standard upgrade inventory (speed / energy cards).</li>
 *   <li>Ejection tri-state per tier: off / configured sides / ME network
 *       (issues.md 其他-10).</li>
 *   <li>Grid: BOTTOM only. All page slots reachable from every face via
 *       ITEM_HANDLER.</li>
 * </ul>
 */
public class EnergizingOrbBlockEntity extends AENetworkBlockEntity
        implements IGridTickable, EnergyAcceptingOrb, IAEPowerStorage, ICraftingMachine,
        IUpgradeableObject, appeng.api.storage.ISubMenuHost {

    /** Legacy NBT key of the pre-0.6.2 boolean auto-export. */
    public static final String NBT_EXPORT = "ap_orb_auto_export";
    public static final String NBT_EJECT = "ap_orb_eject_mode";
    public static final String NBT_EJECT_SIDES = "ap_orb_eject_sides";
    public static final String NBT_THREADS = "ap_orb_threads";
    public static final String NBT_UPGRADES = "ap_orb_upgrades";
    /** AE2 standard upgrade slots for every tier (speed ×5 / energy ×4 caps). */
    public static final int UPGRADE_SLOTS = 5;

    protected final CraftThread[] threads;
    protected final IUpgradeInventory upgrades;
    /** Energy-cell cache only (IO / display / overflow). Never recipe progress. */
    protected long bufferFe;
    /** True when any loaded recipe has no incoming energy (power alert). */
    protected boolean showWarning;
    protected EjectMode ejectMode = EjectMode.OFF;
    protected final EnumSet<Direction> allowedOutputs = EnumSet.noneOf(Direction.class);
    /** Round-robin cursor for cross-page energy distribution. */
    private int energyCursor;
    /** Re-entrancy guard while a page completion writes inventories. */
    protected boolean settling;

    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private LazyOptional<ICraftingMachine> craftMachineCap = LazyOptional.empty();
    protected final MachineSource machineSource = new MachineSource(this);
    private int lastTicks = 1;

    public EnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int pages = pageCount();
        this.threads = new CraftThread[pages];
        for (int i = 0; i < pages; i++) {
            this.threads[i] = new CraftThread(slot -> onThreadSlotChanged(i, slot));
        }
        this.upgrades = UpgradeInventories.forMachine(
                getBlockState().getBlock(), UPGRADE_SLOTS, this::onUpgradesChanged);
        getMainNode()
                .setIdlePowerUsage(0)
                .setExposedOnSides(EnumSet.of(Direction.DOWN))
                .setVisualRepresentation(getItemFromBlockEntity())
                .addService(IGridTickable.class, this);
        try {
            if (APConfig.COMMON.orbAutoExport.get()) {
                ejectMode = EjectMode.NETWORK;
            }
        } catch (Exception e) {
            AppliedPowah.LOG.debug("orbAutoExport config unavailable, default OFF: {}", e.toString());
        }
    }

    /** Pages of this tier. Fluix = 1; Advanced / ME = 4 (explicit 4 threads). */
    public int pageCount() {
        return 1;
    }

    public CraftThread[] getThreads() {        return threads;
    }

    public CraftThread getThread(int index) {
        if (index < 0 || index >= threads.length) {
            return threads[0];
        }
        return threads[index];
    }

    // ---- upgrade inventory (AE2 standard, 5 slots) ----

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public int speedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public int energyCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.ENERGY_CARD);
    }

    protected void onUpgradesChanged() {
        setChanged();
        checkAllRecipes();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    // ---- ejection tri-state ----

    public EjectMode getEjectMode() {
        return ejectMode;
    }

    public void setEjectMode(EjectMode mode) {
        if (mode == null || mode == ejectMode) {
            return;
        }
        this.ejectMode = mode;
        setChanged();
        markForUpdate();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    public void cycleEjectMode() {
        setEjectMode(ejectMode.next());
    }

    public EnumSet<Direction> getAllowedOutputs() {
        return allowedOutputs;
    }

    public boolean isSideAllowed(Direction side) {
        return allowedOutputs.contains(side);
    }

    public void toggleOutputSide(Direction side) {
        if (allowedOutputs.contains(side)) {
            allowedOutputs.remove(side);
        } else {
            allowedOutputs.add(side);
        }
        setChanged();
        markForUpdate();
    }

    /** Bitmask of allowed sides (DOWN=0 … UP=5) for GUI sync. */
    public int allowedSidesMask() {
        int mask = 0;
        for (Direction dir : allowedOutputs) {
            mask |= 1 << dir.ordinal();
        }
        return mask;
    }

    // ---- pages / recipes ----

    protected void onThreadSlotChanged(int page, int slot) {
        if (level != null && !level.isClientSide && !settling) {
            CraftThread t = getThread(page);
            if (t.hasInputs()) {
                checkRecipe(t);
            } else {
                t.resetRecipe();
                t.clearPushContext();
            }
        }
        setChanged();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    /** True while a page is mid-completion (guards re-entrant onContentsChanged). */
    protected boolean isSettling() {
        return settling;
    }

    protected void checkRecipe(CraftThread page) {
        if (level == null || level.isClientSide || settling) {
            return;
        }
        EnergizingRecipe found = EnergizingOrbLogic.findRecipe(level, page.getInv());
        if (found != null) {
            if (page.recipe != found) {
                page.progress = 0;
            }
            page.recipe = found;
            page.energy = recipeEnergyFor(found);
            feedPagesFromBuffer();
        } else {
            page.resetRecipe();
        }
    }

    /** E_need of a recipe — the Powah base energy (speed cards change throughput, not cost). */
    protected long recipeEnergyFor(EnergizingRecipe found) {
        long e0 = found.getEnergy();
        return e0 > 0 ? e0 : 1L;
    }

    protected void checkAllRecipes() {
        for (CraftThread page : threads) {
            if (page.hasInputs()) {
                checkRecipe(page);
            } else {
                page.resetRecipe();
            }
        }
        updateWarning();
    }

    protected boolean anyPageLoaded() {
        for (CraftThread page : threads) {
            if (page.isLoaded()) {
                return true;
            }
        }
        return false;
    }

    protected boolean anyPageHasProduct() {
        for (CraftThread page : threads) {
            if (page.hasProduct()) {
                return true;
            }
        }
        return false;
    }

    /** First page that can take a set of {@code needed} items (round-robin). */
    @Nullable
    protected CraftThread findFreePage(int needed) {
        int n = threads.length;
        for (int k = 0; k < n; k++) {
            CraftThread page = threads[(energyCursor + k) % n];
            if (page.freeSlots() >= needed) {
                return page;
            }
        }
        return null;
    }

    @Nullable
    protected CraftThread findPageFor(List<AEItemKey> singles) {
        int n = threads.length;
        for (int k = 0; k < n; k++) {
            CraftThread page = threads[(energyCursor + k) % n];
            if (page.canAccept(singles)) {
                return page;
            }
        }
        return null;
    }

    // ---- energy distribution across pages (AAE constant-total model) ----

    /**
     * Feed page progress from an energy amount, round-robin.
     *
     * @return FE actually consumed by recipes (rest may go to cache)
     */
    protected long routeEnergyToPages(long amount, int ticks) {
        if (amount <= 0) {
            return 0;
        }
        int ticksSafe = Math.max(1, ticks);
        long remaining = amount;
        int n = threads.length;
        for (int k = 0; k < n && remaining > 0; k++) {
            CraftThread page = threads[(energyCursor + k) % n];
            if (!page.isLoaded() || page.ready()) {
                continue;
            }
            long want = OrbTuning.stepEnergy(page.energy, speedCardCount(), ticksSafe);
            long cap = Math.min(want, page.remaining());
            long take = Math.min(cap, remaining);
            if (take <= 0) {
                continue;
            }
            page.progress = Math.min(page.energy, page.progress + take);
            remaining -= take;
        }
        energyCursor = (energyCursor + 1) % n;
        return amount - remaining;
    }

    /** Cache → page progress (rods/cache must be able to craft). */
    protected void feedPagesFromBuffer() {
        if (bufferFe <= 0) {
            return;
        }
        long used = routeEnergyToPages(bufferFe, lastTicks);
        if (used > 0) {
            bufferFe -= used;
        }
    }

    // ---- completion (unique write path per page) ----

    protected void completeIfPossible(CraftThread page) {
        if (settling || level == null || !page.isLoaded() || !page.ready()) {
            return;
        }
        ItemStack one = EnergizingOrbLogic.resultOf(level, page.recipe);
        if (one.isEmpty()) {
            return;
        }
        ItemStack result = one.copy();
        result.setCount(Math.min(OrbResultRules.MAX_SLOT, one.getCount()));
        if (result.getCount() <= 0 || !canSettleResult(page, result)) {
            return;
        }
        settling = true;
        try {
            // Consume inputs first — onContentsChanged sees no recipe.
            for (int i = 0; i < CraftThread.INPUTS && i < page.getInv().getSlots(); i++) {
                if (!page.getInv().getStackInSlot(i).isEmpty()) {
                    page.getInv().extractItem(i, 1, false);
                }
            }
            settleResult(page, result);
            page.progress = 0;
            page.energy = 0;
            page.recipe = null;
        } finally {
            settling = false;
            page.clearPushContext();
        }
        setChanged();
        markForUpdate();
    }

    /** Whether {@link #settleResult} can place the whole stack without losing items. */
    protected boolean canSettleResult(CraftThread page, ItemStack result) {
        if (result == null || result.isEmpty()) {
            return false;
        }
        ItemStack afterReturn = simulateProviderReturn(page, result);
        boolean sinkTakesAll = afterReturn.isEmpty();
        boolean outputOk = !sinkTakesAll && canPlaceResult(page, afterReturn);
        return OrbResultRules.canSettle(sinkTakesAll, outputOk);
    }

    /**
     * Provider / CPU return first (when the eject mode allows), then the page
     * OUTPUT slot. Never overwrites a different product — refuses and waits.
     */
    protected void settleResult(CraftThread page, ItemStack result) {
        try {
            if (result == null || result.isEmpty()) {
                return;
            }
            ItemStack rest = result;
            if (OrbEjectRules.allowsProviderReturn(ejectMode, page.providerReturnSide != null)) {
                rest = doProviderReturn(page, result);
            }
            page.providerReturnSide = null;
            page.fromCpuOrder = false;
            if (!rest.isEmpty() && !tryPlaceResult(page, rest)) {
                AppliedPowah.LOG.error("Orb settle lost product {} x{} at {} page {}",
                        rest.getItem(), rest.getCount(), worldPosition, pageIndex(page));
                dropStack(rest);
            }
        } finally {
            page.providerReturnSide = null;
            page.fromCpuOrder = false;
        }
    }

    protected int pageIndex(CraftThread page) {
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] == page) {
                return i;
            }
        }
        return -1;
    }

    protected ItemStack simulateProviderReturn(CraftThread page, ItemStack result) {
        if (!OrbEjectRules.allowsProviderReturn(ejectMode, page.providerReturnSide != null)) {
            return result;
        }
        return providerReturn(page, result, Actionable.SIMULATE);
    }

    protected ItemStack doProviderReturn(CraftThread page, ItemStack result) {
        return providerReturn(page, result, Actionable.MODULATE);
    }

    /** Push toward the PatternProvider that issued the set (assembler pushTo). */
    private ItemStack providerReturn(CraftThread page, ItemStack result, Actionable mode) {
        Direction side = page.providerReturnSide;
        if (side == null || level == null || result.isEmpty()) {
            return result;
        }
        try {
            BlockEntity te = level.getBlockEntity(worldPosition.relative(side));
            if (te == null) {
                return result;
            }
            Direction face = side.getOpposite();
            AEItemKey key = AEItemKey.of(result);
            var generic = te.getCapability(Capabilities.GENERIC_INTERNAL_INV, face).orElse(null);
            if (generic != null && key != null && generic.canInsert()) {
                long remaining = result.getCount();
                for (int i = 0; i < generic.size() && remaining > 0; i++) {
                    remaining -= generic.insert(i, key, remaining, mode);
                }
                if (remaining <= 0) {
                    return ItemStack.EMPTY;
                }
                ItemStack left = result.copy();
                left.setCount((int) remaining);
                return left;
            }
            var adaptor = appeng.api.inventories.InternalInventory.wrapExternal(te, face);
            if (adaptor == null) {
                return result;
            }
            return mode == Actionable.SIMULATE ? adaptor.simulateAdd(result) : adaptor.addItems(result);
        } catch (Exception e) {
            AppliedPowah.LOG.warn("provider return {} failed at {} side {}: {}",
                    mode, worldPosition, side, e.toString());
            return result;
        }
    }

    /** OUTPUT empty / same-stack grow / refuse. Never overwrite. */
    protected boolean canPlaceResult(CraftThread page, ItemStack result) {
        if (result == null || result.isEmpty()) {
            return false;
        }
        ItemStack slot = page.getInv().getStackInSlot(CraftThread.OUTPUT);
        return OrbResultRules.canPlace(
                slot.getCount(),
                !slot.isEmpty() && ItemStack.isSameItemSameTags(slot, result),
                result.getCount());
    }

    /** Unique OUTPUT write path. @return false if refused (full / different item). */
    protected boolean tryPlaceResult(CraftThread page, ItemStack result) {
        if (!canPlaceResult(page, result)) {
            return false;
        }
        ItemStack slot = page.getInv().getStackInSlot(CraftThread.OUTPUT);
        if (slot.isEmpty()) {
            page.getInv().setStackInSlot(CraftThread.OUTPUT, result.copy());
            return true;
        }
        ItemStack after = slot.copy();
        after.grow(result.getCount());
        page.getInv().setStackInSlot(CraftThread.OUTPUT, after);
        return true;
    }

    protected void dropStack(ItemStack stack) {
        if (level != null && !stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                    worldPosition.getZ(), stack);
        }
    }

    // ---- ICraftingMachine: AE2 PatternProvider pushes full sets ----

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        AEItemKey icon = AEItemKey.of(new ItemStack(getItemFromBlockEntity()));
        Component name = getBlockState().getBlock().getName();
        return new PatternContainerGroup(icon, name, List.of());
    }

    @Override
    public boolean acceptsPlans() {
        if (level == null || level.isClientSide) {
            return false;
        }
        for (CraftThread page : threads) {
            if (page.freeSlots() > 0 && !page.hasInputs()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (patternDetails == null || inputs == null || level == null || level.isClientSide) {
            return false;
        }
        List<AEItemKey> singles = expandPatternSingles(inputs);
        if (singles == null || singles.isEmpty() || singles.size() > CraftThread.INPUTS) {
            return false;
        }
        CraftThread page = findPageFor(singles);
        if (page == null || !page.accept(singles)) {
            return false;
        }
        page.providerReturnSide = ejectionDirection;
        page.fromCpuOrder = false;
        checkRecipe(page);
        setChanged();
        markForUpdate();
        return true;
    }

    @Nullable
    private static List<AEItemKey> expandPatternSingles(KeyCounter[] inputHolders) {
        List<AEItemKey> singles = new ArrayList<>();
        for (KeyCounter counter : inputHolders) {
            if (counter == null || counter.isEmpty()) {
                continue;
            }
            for (var entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) {
                    return null;
                }
                for (long n = 0; n < entry.getLongValue(); n++) {
                    singles.add(itemKey);
                }
            }
        }
        return singles;
    }

    // ---- ejection passes ----

    protected int autoExportBudget() {
        try {
            return Math.max(1, APConfig.COMMON.orbExportItemsPerTick.get());
        } catch (Exception e) {
            AppliedPowah.LOG.debug("orbExportItemsPerTick config unavailable, default 8: {}", e.toString());
            return 8;
        }
    }

    private void ejectTick(IGridNode node) {
        if (!ejectMode.ejects()) {
            return;
        }
        boolean worked = false;
        if (OrbEjectRules.allowsNetworkPush(ejectMode)) {
            worked |= pushOutToMe(node);
        }
        if (ejectMode == EjectMode.SIDES) {
            worked |= pushOutToSides();
        }
        if (worked) {
            setChanged();
            markForUpdate();
        }
    }

    /** Push page outputs into ME storage (old autoExport ON semantics). */
    protected boolean pushOutToMe(IGridNode node) {
        if (node == null || !node.isActive() || !ModList.get().isLoaded("ae2")) {
            return false;
        }
        boolean moved = false;
        try {
            IStorageService storageService = node.getGrid().getService(IStorageService.class);
            IEnergyService energyService = node.getGrid().getEnergyService();
            if (storageService == null || energyService == null) {
                return false;
            }
            for (CraftThread page : threads) {
                ItemStack still = page.getInv().getStackInSlot(CraftThread.OUTPUT);
                if (still.isEmpty()) {
                    continue;
                }
                AEItemKey key = AEItemKey.of(still);
                if (key == null) {
                    continue;
                }
                long sim = StorageHelper.poweredInsert(
                        energyService, storageService.getInventory(), key, still.getCount(),
                        machineSource, Actionable.SIMULATE);
                if (sim <= 0) {
                    continue;
                }
                long real = StorageHelper.poweredInsert(
                        energyService, storageService.getInventory(), key, sim, machineSource);
                if (real > 0) {
                    page.getInv().extractItem(CraftThread.OUTPUT, (int) real, false);
                    moved = true;
                }
            }
        } catch (Exception e) {
            AppliedPowah.LOG.warn("pushOutToMe failed at {}: {}", worldPosition, e.toString());
        }
        return moved;
    }

    /** Push page outputs into adjacent inventories on the configured faces. */
    protected boolean pushOutToSides() {
        if (level == null || allowedOutputs.isEmpty()) {
            return false;
        }
        int budget = autoExportBudget();
        boolean moved = false;
        for (Direction dir : allowedOutputs) {
            if (budget <= 0) {
                break;
            }
            BlockPos targetPos = worldPosition.relative(dir);
            var targetBe = level.getBlockEntity(targetPos);
            if (targetBe == null) {
                continue;
            }
            IItemHandler target = targetBe.getCapability(
                    ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
            if (target == null) {
                continue;
            }
            for (CraftThread page : threads) {
                if (budget <= 0) {
                    break;
                }
                ItemStack out = page.getInv().getStackInSlot(CraftThread.OUTPUT);
                if (out.isEmpty()) {
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
                ItemStack extracted = page.getInv().extractItem(CraftThread.OUTPUT, can, false);
                ItemStack rem = ItemHandlerHelper.insertItem(target, extracted, false);
                if (!rem.isEmpty()) {
                    ItemStack back = page.getInv().insertItem(CraftThread.OUTPUT, rem, false);
                    if (!back.isEmpty()) {
                        dropStack(back);
                    }
                }
                budget -= can;
                moved = true;
            }
        }
        return moved;
    }

    // ---- warning / display ----

    /**
     * reaction_chamber-style power alert: a loaded recipe with an empty cache
     * and no incoming energy. ME/Advanced tiers only warn when rods/cache exist.
     */
    protected void updateWarning() {
        boolean warn = false;
        if (anyPageLoaded() && bufferFe <= 0) {
            for (CraftThread page : threads) {
                if (page.isLoaded() && page.progress <= 0) {
                    warn = true;
                    break;
                }
            }
        }
        if (warn != showWarning) {
            showWarning = warn;
        }
    }

    public boolean isShowWarning() {
        return showWarning;
    }

    public boolean isAeDisplay() {
        return false;
    }

    public long getDisplayEnergy() {
        return 0;
    }

    public long getDisplayCapacity() {
        return 0;
    }

    public String getEnergyUnit() {
        return isAeDisplay() ? "AE" : "FE";
    }

    // ---- Jade / AE2 IAEPowerStorage — ME orb has no cache (0/0). ----

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

    /** Base orb: never extract AE/FE from the network (Advanced overrides). */
    protected void pullFromNetwork(IGridNode node) {
    }

    // ---- ticking ----

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        // canBeAlerted=true — inventory / returnInv changes call alertDevice.
        return new TickingRequest(1, 20, false, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) {
            return TickRateModulation.SLEEP;
        }
        lastTicks = Math.max(1, ticksSinceLastCall);
        for (CraftThread page : threads) {
            if (page.hasInputs()) {
                checkRecipe(page);
            } else if (page.isLoaded()) {
                page.resetRecipe();
                page.clearPushContext();
            }
        }
        feedPagesFromBuffer();
        pullFromNetwork(node);
        for (CraftThread page : threads) {
            if (page.ready()) {
                completeIfPossible(page);
            }
        }
        feedPagesFromBuffer();
        ejectTick(node);
        updateWarning();
        boolean work = anyPageLoaded() || anyPageHasProduct() || ejectMode.ejects() || wantsWork();
        return work ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    /** Extra keep-awake conditions for subclasses (cache fill, rods present). */
    protected boolean wantsWork() {
        return false;
    }

    // ---- rod feed (PowahBridge / ApOrbFeed entry point) ----

    @Override
    public long fillEnergy(long amount) {
        if (level == null || amount <= 0 || settling) {
            return 0;
        }
        long filled = routeEnergyToPages(amount, 1);
        setChanged();
        markForUpdate();
        return filled;
    }

    // ---- grid connectivity: BOTTOM only (issues 0.6.0-7) ----

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

    // ---- NBT ----

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("bufferFe", bufferFe);
        tag.putBoolean("showWarning", showWarning);
        tag.putInt(NBT_EJECT, ejectMode.ordinal());
        int mask = allowedSidesMask();
        if (mask != 0) {
            tag.putInt(NBT_EJECT_SIDES, mask);
        }
        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        for (CraftThread page : threads) {
            CompoundTag one = new CompoundTag();
            page.writeNBT(one);
            pages.add(one);
        }
        tag.put(NBT_THREADS, pages);
        upgrades.writeToNBT(tag, NBT_UPGRADES);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        bufferFe = tag.getLong("bufferFe");
        showWarning = tag.getBoolean("showWarning");
        // Legacy: boolean auto-export → NETWORK when true.
        if (tag.contains(NBT_EJECT)) {
            int ordinal = tag.getInt(NBT_EJECT);
            EjectMode[] values = EjectMode.values();
            ejectMode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : EjectMode.OFF;
        } else {
            ejectMode = tag.getBoolean(NBT_EXPORT) ? EjectMode.NETWORK : EjectMode.OFF;
        }
        allowedOutputs.clear();
        if (tag.contains(NBT_EJECT_SIDES)) {
            int mask = tag.getInt(NBT_EJECT_SIDES);
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.ordinal())) != 0) {
                    allowedOutputs.add(dir);
                }
            }
        }
        if (tag.contains(NBT_THREADS)) {
            net.minecraft.nbt.ListTag pages = tag.getList(NBT_THREADS, CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < pages.size() && i < threads.length; i++) {
                threads[i].readNBT(pages.getCompound(i));
            }
        } else if (tag.contains("orbInv")) {
            // 0.6.1 migration: single 7-slot inventory → page 0.
            migrateLegacyInv(tag.getCompound("orbInv"));
        }
        upgrades.readFromNBT(tag, NBT_UPGRADES);
        checkAllRecipes();
    }

    private void migrateLegacyInv(CompoundTag legacy) {
        ItemStackHandler tmp = new ItemStackHandler(7);
        tmp.deserializeNBT(legacy);
        CraftThread page = threads[0];
        for (int i = 1; i <= CraftThread.INPUTS && i < tmp.getSlots(); i++) {
            ItemStack stack = tmp.getStackInSlot(i);
            page.getInv().setStackInSlot(i - 1, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        page.getInv().setStackInSlot(CraftThread.OUTPUT, tmp.getStackInSlot(0).copy());
    }

    // ---- capabilities ----

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (!itemCap.isPresent()) {
                IItemHandler[] views = new IItemHandler[threads.length];
                for (int i = 0; i < threads.length; i++) {
                    views[i] = threads[i].externalView();
                }
                itemCap = LazyOptional.of(() -> new CombinedItemHandler(views));
            }
            return itemCap.cast();
        }
        if (cap == Capabilities.CRAFTING_MACHINE) {
            if (!craftMachineCap.isPresent()) {
                craftMachineCap = LazyOptional.of(() -> this);
            }
            return craftMachineCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        craftMachineCap.invalidate();
    }

    // ---- ISubMenuHost (direction config sub-screen) ----

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getItemFromBlockEntity());
    }

    @Override
    public void returnToMainMenu(Player player, appeng.menu.ISubMenu subMenu) {
        appeng.menu.MenuOpener.returnTo(APOrbs.ORB_MENU.get(), player,
                appeng.menu.locator.MenuLocators.forBlockEntity(this));
    }

    // ---- drops ----

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        for (CraftThread page : threads) {
            for (int i = 0; i < page.getInv().getSlots(); i++) {
                page.getInv().setStackInSlot(i, ItemStack.EMPTY);
            }
            page.resetRecipe();
            page.clearPushContext();
        }
        upgrades.clear();
    }

    @Override
    public boolean containRecipe() {
        return anyPageLoaded();
    }
}
