package com.coala.appliedpowah.orb;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 6+1 task slots; Advanced orb adds 4 rod slots. Output is read-only for players.
 *
 * Client GUI must read progress / warning / autoExport from DataSlots —
 * the client BlockEntity is not guaranteed to carry live buffer values.
 */
public class EnergizingOrbMenu extends AbstractContainerMenu {

    private final MeEnergizingOrbBlockEntity orb;
    private final ContainerLevelAccess access;
    private final int orbSlots;

    // Synced to client via DataSlot (server get() → client set())
    private int syncProgress;
    private int syncRecipeEnergy;
    private int syncAutoExport;
    private int syncAdvEnergy;
    private int syncAdvCapacity;

    public EnergizingOrbMenu(int id, Inventory playerInv, MeEnergizingOrbBlockEntity orb) {
        super(APOrbs.ORB_MENU.get(), id);
        this.orb = orb;
        this.access = ContainerLevelAccess.create(orb.getLevel(), orb.getBlockPos());
        ItemStackHandler inv = orb.getInv();
        boolean adv = orb instanceof AdvancedEnergizingOrbBlockEntity;
        // Output slot (centered in 26×26 output area) — shifted +1,+1 per user spec
        int outX = adv ? 113 : 124;
        int outY = 48;
        this.addSlot(new SlotItemHandler(inv, 0, outX, outY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // Input slots (2×3 grid) — shifted +1,+1
        int[][] in = adv
                ? new int[][]{{14, 30}, {32, 30}, {32, 48}, {14, 48}, {14, 66}, {32, 66}}
                : new int[][]{{25, 30}, {43, 30}, {43, 48}, {25, 48}, {25, 66}, {43, 66}};
        for (int i = 0; i < 6; i++) {
            this.addSlot(new SlotItemHandler(inv, 1 + i, in[i][0], in[i][1]));
        }
        int count = 7;
        if (adv) {
            AdvancedEnergizingOrbBlockEntity aorb = (AdvancedEnergizingOrbBlockEntity) orb;
            ItemStackHandler rods = aorb.getRodInv();
            for (int i = 0; i < AdvancedEnergizingOrbBlockEntity.ROD_SLOTS; i++) {
                this.addSlot(new SlotItemHandler(rods, i, 153, 22 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof com.coala.appliedpowah.chargingrod.RodBlockItem
                                && super.mayPlace(stack);
                    }
                });
            }
            count += AdvancedEnergizingOrbBlockEntity.ROD_SLOTS;
        }
        this.orbSlots = count;
        // Player inventory — shifted +1,+1 from YAML spec
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 115 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 173));
        }

        // Live GUI sync — progress bar animates every tick these change
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) Math.min(Integer.MAX_VALUE, orb.getProgress());
            }

            @Override
            public void set(int value) {
                syncProgress = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) Math.min(Integer.MAX_VALUE, orb.getRecipeEnergy());
            }

            @Override
            public void set(int value) {
                syncRecipeEnergy = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return orb.isAutoExport() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                syncAutoExport = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) Math.min(Integer.MAX_VALUE, orb.getDisplayEnergy());
            }

            @Override
            public void set(int value) {
                syncAdvEnergy = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) Math.min(Integer.MAX_VALUE, orb.getDisplayCapacity());
            }

            @Override
            public void set(int value) {
                syncAdvCapacity = value;
            }
        });
    }

    public static EnergizingOrbMenu clientFactory(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        net.minecraft.world.level.Level level = inv.player.getCommandSenderWorld();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MeEnergizingOrbBlockEntity orb) {
            return new EnergizingOrbMenu(id, inv, orb);
        }
        throw new IllegalStateException("Energizing orb missing at " + pos);
    }

    /** GUI-facing: DataSlot value on client, live BE value on server. */
    public long getGuiProgress() {
        return syncProgress;
    }

    public long getGuiRecipeEnergy() {
        return syncRecipeEnergy;
    }

    public boolean getGuiAutoExport() {
        return syncAutoExport != 0;
    }

    public long getGuiEnergy() {
        return syncAdvEnergy;
    }

    public long getGuiCapacity() {
        return syncAdvCapacity;
    }

    public String getGuiEnergyUnit() {
        return orb instanceof AdvancedEnergizingOrbBlockEntity adv && adv.isAeDisplay() ? "AE" : "FE";
    }

    public boolean isAdvanced() {
        return orb instanceof AdvancedEnergizingOrbBlockEntity;
    }

    public boolean isAuto() {
        return orb instanceof AutoEnergizingOrbBlockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < orbSlots) {
                if (!this.moveItemStackTo(stack, orbSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 1, orbSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return orb != null && stillValid(access, player, orb.getBlockState().getBlock());
    }

    public MeEnergizingOrbBlockEntity getOrb() {
        return orb;
    }
}
