package com.coala.appliedpowah.orb;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 6-slot universal upgrade inventory (开发总报告 §3.3): thread / parallel / energy
 * cards only; not slot-bound — caps by card count (thread Σ≤1 family, parallel Σ≤1
 * family, energy ≤4). Cards never stack in these slots.
 */
public class UpgradeCardInv extends ItemStackHandler {

    public static final int SLOTS = 6;
    public static final int THREAD_MAX = 1;
    public static final int PARALLEL_MAX = 1;
    public static final int ENERGY_MAX = OrbTuning.ENERGY_CARD_MAX;

    public UpgradeCardInv() {
        super(SLOTS);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInstall(stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!canInstall(stack)) {
            return stack;
        }
        ItemStack single = stack.copy();
        single.setCount(1);
        ItemStack left = super.insertItem(slot, single, true);
        if (!left.isEmpty()) {
            return stack;
        }
        if (simulate) {
            return stack.getCount() <= 1 ? ItemStack.EMPTY : shrink(stack, 1);
        }
        super.insertItem(slot, single, false);
        return stack.getCount() <= 1 ? ItemStack.EMPTY : shrink(stack, 1);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!stack.isEmpty() && stack.getCount() > 1) {
            ItemStack single = stack.copy();
            single.setCount(1);
            super.setStackInSlot(slot, single);
            return;
        }
        super.setStackInSlot(slot, stack);
    }

    private static ItemStack shrink(ItemStack stack, int n) {
        ItemStack rest = stack.copy();
        rest.shrink(n);
        return rest;
    }

    /** Family-cap insert filter: allow only while under that family's max. */
    public boolean canInstall(ItemStack stack) {
        if (stack.isEmpty() || !UpgradeCardItem.isUpgradeCard(stack)) {
            return false;
        }
        if (UpgradeCardItem.isThreadCard(stack)) {
            return countThread() < THREAD_MAX;
        }
        if (UpgradeCardItem.isParallelCard(stack)) {
            return countParallel() < PARALLEL_MAX;
        }
        return countEnergy() < ENERGY_MAX;
    }

    /** Σ thread-card bonuses (1/2/4/8); 0 if none. */
    public int countThread() {
        int n = 0;
        for (int i = 0; i < getSlots(); i++) {
            ItemStack s = getStackInSlot(i);
            if (UpgradeCardItem.isThreadCard(s)) {
                n++;
            }
        }
        return n;
    }

    public int threadBonus() {
        int bonus = 0;
        for (int i = 0; i < getSlots(); i++) {
            ItemStack s = getStackInSlot(i);
            if (UpgradeCardItem.isThreadCard(s) && s.getItem() instanceof UpgradeCardItem c) {
                bonus += c.getValue();
            }
        }
        return bonus;
    }

    public int countParallel() {
        int n = 0;
        for (int i = 0; i < getSlots(); i++) {
            if (UpgradeCardItem.isParallelCard(getStackInSlot(i))) {
                n++;
            }
        }
        return n;
    }

    /** Max parallel card value installed (only one allowed). */
    public int parallelValue() {
        for (int i = 0; i < getSlots(); i++) {
            ItemStack s = getStackInSlot(i);
            if (UpgradeCardItem.isParallelCard(s) && s.getItem() instanceof UpgradeCardItem c) {
                return c.getValue();
            }
        }
        return 1;
    }

    public int countEnergy() {
        int n = 0;
        for (int i = 0; i < getSlots(); i++) {
            if (UpgradeCardItem.isEnergyCard(getStackInSlot(i))) {
                n++;
            }
        }
        return n;
    }
}
