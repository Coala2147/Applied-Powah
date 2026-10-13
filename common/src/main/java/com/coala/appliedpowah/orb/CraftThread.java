package com.coala.appliedpowah.orb;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;
import owmii.powah.block.energizing.EnergizingRecipe;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * One craft page (thread) — ExtendedAE {@code CraftingThread} analogue.
 * Holds 6 input slots + 1 output slot + its own recipe progress.
 *
 * <p>Invariants (issues.md 1.5 / 其他-3):</p>
 * <ul>
 *   <li>one completion consumes exactly one recipe input set and produces exactly
 *       {@code result × count} — never a multiplier;</li>
 *   <li>output write path is empty / same-stack grow / refuse-and-wait;</li>
 *   <li>a page accepts a set all-or-nothing (≤6 items → ≤6 free slots).</li>
 * </ul>
 */
public final class CraftThread {

    public static final int INPUTS = 6;
    public static final int OUTPUT = INPUTS;
    public static final int SIZE = INPUTS + 1;

    private final ItemStackHandler inv;
    /** Routed change notifications back to the owning BlockEntity. */
    private final IntConsumer onChange;

    /** FE poured into the loaded recipe (0..energy). */
    public long progress;
    /** E_need of the loaded recipe (Powah base energy). 0 = no recipe loaded. */
    public long energy;
    /** Resolved recipe — transient, re-matched from the inputs after NBT load. */
    @Nullable
    public transient EnergizingRecipe recipe;
    /** Side the current input set was pushed from (ICraftingMachine). Transient. */
    @Nullable
    public transient Direction providerReturnSide;
    /** True while the current set came from a ME crafting order. Transient. */
    public transient boolean fromCpuOrder;

    public CraftThread(IntConsumer onChange) {
        this.onChange = onChange;
        this.inv = new ItemStackHandler(SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                onChange.accept(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == OUTPUT
                        || EnergizingOrbLogic.canInsertInput(this, slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == OUTPUT ? 64 : 1;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot != OUTPUT && !stack.isEmpty()) {
                    ItemStack single = stack.copy();
                    single.setCount(1);
                    if (!super.insertItem(slot, single, true).isEmpty()) {
                        return stack;
                    }
                    if (simulate) {
                        return shrinkCopy(stack, 1);
                    }
                    super.insertItem(slot, single, false);
                    return shrinkCopy(stack, 1);
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                if (slot != OUTPUT && !stack.isEmpty() && stack.getCount() > 1) {
                    ItemStack single = stack.copy();
                    single.setCount(1);
                    super.setStackInSlot(slot, single);
                    return;
                }
                super.setStackInSlot(slot, stack);
            }

            private ItemStack shrinkCopy(ItemStack stack, int n) {
                if (stack.getCount() <= n) {
                    return ItemStack.EMPTY;
                }
                ItemStack rest = stack.copy();
                rest.shrink(n);
                return rest;
            }
        };
    }

    public ItemStackHandler getInv() {
        return inv;
    }

    /** External-access view (all faces): inputs 0..5, output 6. */
    public IItemHandler externalView() {
        return new RangedWrapper(inv, 0, SIZE);
    }

    public int filledInputs() {
        int n = 0;
        for (int i = 0; i < INPUTS && i < inv.getSlots(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public boolean hasInputs() {
        return filledInputs() > 0;
    }

    public boolean hasProduct() {
        return !inv.getStackInSlot(OUTPUT).isEmpty();
    }

    /** Idle = no inputs and no product; energy fields can be stale-ignored. */
    public boolean isIdle() {
        return !hasInputs() && !hasProduct();
    }

    public boolean isLoaded() {
        return energy > 0;
    }

    public boolean ready() {
        return energy > 0 && progress >= energy;
    }

    public long remaining() {
        return energy > 0 ? Math.max(0L, energy - progress) : 0L;
    }

    public int progressPercent() {
        if (energy <= 0) {
            return 0;
        }
        return (int) Math.min(100, progress * 100 / energy);
    }

    /** Number of input slots still free. */
    public int freeSlots() {
        int n = 0;
        for (int i = 0; i < INPUTS && i < inv.getSlots(); i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** All-or-nothing probe: can this page take the whole flattened set? */
    public boolean canAccept(List<appeng.api.stacks.AEItemKey> singles) {
        if (singles == null || singles.isEmpty() || singles.size() > INPUTS) {
            return false;
        }
        if (hasInputs() || freeSlots() < singles.size()) {
            return false;
        }
        for (var key : singles) {
            boolean ok = false;
            for (int i = 0; i < INPUTS; i++) {
                if (EnergizingOrbLogic.canInsertInput(inv, i, key.toStack(1))) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Fill the page with one complete input set (caller checked {@link #canAccept}). */
    public boolean accept(List<appeng.api.stacks.AEItemKey> singles) {
        if (!canAccept(singles)) {
            return false;
        }
        int idx = 0;
        for (int i = 0; i < INPUTS && idx < singles.size(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                continue;
            }
            inv.setStackInSlot(i, singles.get(idx).toStack(1));
            idx++;
        }
        return idx == singles.size();
    }

    /** Drop the loaded recipe state; keeps inventories. */
    public void resetRecipe() {
        recipe = null;
        energy = 0;
        progress = 0;
    }

    /** Clear transient push context (after settling or refusing). */
    public void clearPushContext() {
        providerReturnSide = null;
        fromCpuOrder = false;
    }

    public void writeNBT(CompoundTag tag) {
        tag.put("pageInv", inv.serializeNBT());
        tag.putLong("progress", progress);
        tag.putLong("energy", energy);
    }

    public void readNBT(CompoundTag tag) {
        progress = tag.getLong("progress");
        energy = tag.getLong("energy");
        recipe = null;
        providerReturnSide = null;
        fromCpuOrder = false;
        if (tag.contains("pageInv")) {
            inv.deserializeNBT(tag.getCompound("pageInv"));
        }
    }
}
