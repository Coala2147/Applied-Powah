package com.coala.appliedpowah.orb;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One virtual craft lane (开发总报告 §3.6). Physical input slots show lane 0;
 * extra lanes hold their own input sets (no stacking into the 1-limit slots).
 * Each lane fills {@code progress → eNeed} and completes {@code batch} (=P) copies.
 */
public final class CraftLane {

    public final List<ItemStack> inputs = new ArrayList<>();
    public long recipeEnergy;
    public long recipeProgress;
    public int batch = 1;
    public boolean active;

    /** Display name of the expected result (for GUI status). */
    public String statusKey = "";

    public void clear() {
        inputs.clear();
        recipeEnergy = 0;
        recipeProgress = 0;
        batch = 1;
        active = false;
        statusKey = "";
    }

    public void start(List<ItemStack> set, long eNeed, int parallel, String key) {
        inputs.clear();
        for (ItemStack s : set) {
            inputs.add(s.copy());
        }
        recipeEnergy = eNeed;
        recipeProgress = 0;
        batch = Math.max(1, parallel);
        active = true;
        statusKey = key == null ? "" : key;
    }

    public boolean ready() {
        return active && recipeEnergy > 0 && recipeProgress >= recipeEnergy;
    }

    /** @return FE absorbed */
    public long fill(long amount) {
        if (!active || recipeEnergy <= 0 || amount <= 0) {
            return 0;
        }
        long need = Math.max(0L, recipeEnergy - recipeProgress);
        long filled = Math.min(need, amount);
        recipeProgress += filled;
        return filled;
    }

    public int progressPercent() {
        if (recipeEnergy <= 0) {
            return 0;
        }
        return (int) Math.min(100, recipeProgress * 100 / recipeEnergy);
    }

    /** Copy of one input set (for complete consumption). */
    public List<ItemStack> copyInputs() {
        List<ItemStack> out = new ArrayList<>(inputs.size());
        for (ItemStack s : inputs) {
            out.add(s.copy());
        }
        return out;
    }
}
