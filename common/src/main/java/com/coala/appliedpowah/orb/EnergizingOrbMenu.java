package com.coala.appliedpowah.orb;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 6+1 task slots; Advanced orb adds 4 rod slots. Output is read-only for players.
 */
public class EnergizingOrbMenu extends AbstractContainerMenu {

    private final MeEnergizingOrbBlockEntity orb;
    private final ContainerLevelAccess access;
    private final int orbSlots;

    public EnergizingOrbMenu(int id, Inventory playerInv, MeEnergizingOrbBlockEntity orb) {
        super(APOrbs.ORB_MENU.get(), id);
        this.orb = orb;
        this.access = ContainerLevelAccess.create(orb.getLevel(), orb.getBlockPos());
        ItemStackHandler inv = orb.getInv();
        this.addSlot(new SlotItemHandler(inv, 0, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        int[][] in = {{44, 17}, {62, 17}, {80, 17}, {44, 35}, {62, 35}, {80, 35}};
        for (int i = 0; i < 6; i++) {
            this.addSlot(new SlotItemHandler(inv, 1 + i, in[i][0], in[i][1]));
        }
        int count = 7;
        if (orb instanceof AdvancedEnergizingOrbBlockEntity adv) {
            ItemStackHandler rods = adv.getRodInv();
            // Right-hand vertical rod column (matches with-4-block GUI mock)
            for (int i = 0; i < AdvancedEnergizingOrbBlockEntity.ROD_SLOTS; i++) {
                this.addSlot(new SlotItemHandler(rods, i, 143, 14 + i * 18) {
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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
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
