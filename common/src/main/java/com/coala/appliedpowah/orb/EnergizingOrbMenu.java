package com.coala.appliedpowah.orb;

import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-page energizing orb menu (ExtendedAE ex-molecular-assembler layout).
 *
 * <ul>
 *   <li>one custom {@link appeng.menu.SlotSemantic} per page (inputs) plus shared
 *       {@link SlotSemantics#MACHINE_OUTPUT}; the ScreenStyle JSON places them, so all
 *       pages overlap and only the active page is drawn / clickable;</li>
 *   <li>AE2 standard 5-slot upgrade inventory (speed / energy cards) via
 *       {@link UpgradeableMenu#setupUpgrades()};</li>
 *   <li>Advanced / ME tiers add 4 rod slots;</li>
 *   <li>ejection tri-state and face configuration are client actions handled on the
 *       server-side BlockEntity.</li>
 * </ul>
 */
public class EnergizingOrbMenu extends UpgradeableMenu<EnergizingOrbBlockEntity>
        implements IProgressProvider, ISubMenu {

    /** Upgrade slots are registered first by {@link UpgradeableMenu#setupUpgrades()}. */
    public static final int UPGRADE_SLOTS = EnergizingOrbBlockEntity.UPGRADE_SLOTS;

    private static final String ACTION_SHOW_PAGE = "showPage";
    private static final String ACTION_CYCLE_EJECT = "cycleEject";
    private static final String ACTION_TOGGLE_SIDE = "toggleSide";

    // GuiSync indices — 0/1/5/6 belong to UpgradeableMenu (redstone/fuzzy/crafting/scheduling).
    @GuiSync(2)
    public int page;
    @GuiSync(3)
    public EjectMode ejectMode = EjectMode.OFF;
    @GuiSync(4)
    public int ejectSides;
    @GuiSync(7)
    public int currentProgress;
    @GuiSync(8)
    public int maxProgress;
    @GuiSync(9)
    public boolean showWarning;
    @GuiSync(10)
    public int guiEnergy;
    @GuiSync(11)
    public int guiEnergyCapacity;
    @GuiSync(12)
    public boolean guiAeUnit;

    private final EnergizingOrbBlockEntity orb;
    /** Slots of every page, in registration order (6 inputs then output). */
    private final List<List<Slot>> pageSlots = new ArrayList<>();
    private final int firstPlayerSlot;

    public EnergizingOrbMenu(MenuType<?> type, int id, Inventory playerInv, EnergizingOrbBlockEntity host) {
        super(type, id, playerInv, host);
        this.orb = host;
        ApSlotSemantics.touch();

        boolean adv = host instanceof AdvancedEnergizingOrbBlockEntity;
        int pages = host.pageCount();
        for (int p = 0; p < pages; p++) {
            List<Slot> slots = new ArrayList<>(CraftThread.SIZE);
            ItemStackHandler inv = host.getThread(p).getInv();
            for (int i = 0; i < CraftThread.INPUTS; i++) {
                slots.add(this.addSlot(new OrbInputSlot(inv, i, p), ApSlotSemantics.page(p)));
            }
            slots.add(this.addSlot(
                    new OrbOutputSlot(inv, CraftThread.OUTPUT, p), SlotSemantics.MACHINE_OUTPUT));
            pageSlots.add(slots);
        }
        if (adv) {
            ItemStackHandler rods = ((AdvancedEnergizingOrbBlockEntity) host).getRodInv();
            for (int i = 0; i < AdvancedEnergizingOrbBlockEntity.ROD_SLOTS; i++) {
                this.addSlot(new OrbRodSlot(rods, i), ApSlotSemantics.ROD);
            }
        }
        this.firstPlayerSlot = this.slots.size();

        this.page = 0;
        this.ejectMode = host.getEjectMode();
        this.ejectSides = host.allowedSidesMask();
        registerClientActions();
        showPage();
        broadcastState();
    }

    private void registerClientActions() {
        registerClientAction(ACTION_SHOW_PAGE, Integer.class, p -> {
            this.page = clampPage(p);
            showPage();
        });
        registerClientAction(ACTION_CYCLE_EJECT, () -> orb.cycleEjectMode());
        registerClientAction(ACTION_TOGGLE_SIDE, Direction.class, orb::toggleOutputSide);
    }

    // ---- pages ----

    public int getPage() {
        return page;
    }

    public int pageCount() {
        return orb.pageCount();
    }

    public boolean isPageActive(int p) {
        return p == page;
    }

    private int clampPage(int p) {
        return Math.max(0, Math.min(pageCount() - 1, p));
    }

    /** Client-side page switch: apply locally, then ask the server to follow. */
    public void requestPage(int p) {
        this.page = clampPage(p);
        showPage();
        if (isClientSide()) {
            sendClientAction(ACTION_SHOW_PAGE, this.page);
        }
    }

    /** Only the active page draws and accepts clicks (hidden slots stay inactive). */
    public void showPage() {
        for (int p = 0; p < pageSlots.size(); p++) {
            boolean active = p == page;
            for (Slot slot : pageSlots.get(p)) {
                if (slot instanceof OrbSlot orbSlot) {
                    orbSlot.setPageActive(active);
                }
            }
        }
    }

    // ---- ejection tri-state ----

    public EjectMode getEjectMode() {
        return ejectMode;
    }

    public boolean isSideAllowed(Direction side) {
        return (ejectSides & (1 << side.ordinal())) != 0;
    }

    /** Toolbar button: cycle OFF → SIDES → NETWORK (server applies, client mirrors). */
    public void cycleEject() {
        this.ejectMode = ejectMode.next();
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_EJECT);
        }
    }

    /** Directions sub-screen checkbox (optimistic local echo, server confirms). */
    public void toggleSide(Direction side) {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_SIDE, side);
            this.ejectSides ^= 1 << side.ordinal();
        }
    }

    public boolean showsDirectionsButton() {
        return ejectMode == EjectMode.SIDES;
    }

    // ---- progress / energy (GuiSync) ----

    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public long getGuiProgress() {
        return currentProgress;
    }

    public long getGuiRecipeEnergy() {
        return maxProgress;
    }

    public long getGuiEnergy() {
        return guiEnergy;
    }

    public long getGuiCapacity() {
        return guiEnergyCapacity;
    }

    public String getGuiEnergyUnit() {
        return guiAeUnit ? "AE" : "FE";
    }

    public boolean isShowWarning() {
        return showWarning;
    }

    public boolean isAuto() {
        return orb instanceof com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity;
    }

    public boolean isAdvanced() {
        return orb instanceof AdvancedEnergizingOrbBlockEntity;
    }

    private void broadcastState() {
        CraftThread t = orb.getThread(this.page);
        this.currentProgress = clampInt(t.progress);
        this.maxProgress = clampInt(t.energy);
        this.showWarning = orb.isShowWarning();
        this.guiEnergy = clampInt(orb.getDisplayEnergy());
        this.guiEnergyCapacity = clampInt(orb.getDisplayCapacity());
        this.guiAeUnit = orb.isAeDisplay();
    }

    @Override
    protected void standardDetectAndSendChanges() {
        if (isServerSide()) {
            broadcastState();
            this.ejectMode = orb.getEjectMode();
            this.ejectSides = orb.allowedSidesMask();
        }
        super.standardDetectAndSendChanges();
    }

    private static int clampInt(long v) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, v));
    }

    // ISubMenu: EnergizingOrbBlockEntity implements ISubMenuHost, so the inherited
    // final UpgradeableMenu#getHost() satisfies the interface via a covariant bridge.

    // ---- vanilla plumbing ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < firstPlayerSlot) {
            if (!this.moveItemStackTo(stack, firstPlayerSlot, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, UPGRADE_SLOTS, firstPlayerSlot, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(orb.getLevel(), orb.getBlockPos()),
                player, orb.getBlockState().getBlock());
    }

    public EnergizingOrbBlockEntity getOrb() {
        return orb;
    }

    /** Tier tag carried by the three MenuTypes. */
    public enum Tier {
        FLUIX,
        ADVANCED,
        ME
    }

    /** One shared client factory for all three tiers. */
    public static EnergizingOrbMenu clientFactory(Tier tier, int id, Inventory inv,
            FriendlyByteBuf buf) {
        MenuType<?> type = switch (tier) {
            case FLUIX -> APOrbs.FLUIX_ORB_MENU.get();
            case ADVANCED -> APOrbs.ADV_ORB_MENU.get();
            case ME -> APOrbs.ME_ORB_MENU.get();
        };
        BlockPos pos = buf.readBlockPos();
        net.minecraft.world.level.Level level = inv.player.getCommandSenderWorld();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnergizingOrbBlockEntity orb) {
            return new EnergizingOrbMenu(type, id, inv, orb);
        }
        throw new IllegalStateException("Energizing orb missing at " + pos);
    }
}
