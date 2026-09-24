package com.coala.appliedpowah.orb;

/**
 * Pure recipe-progress / cache energy split (开发总报告 §1.1 / §3.1).
 * No Minecraft types — unit-testable.
 *
 * <ul>
 *   <li>{@code recipeProgress} fills toward {@code recipeEnergy} and is <b>not</b>
 *       capped by the energy-cell cache.</li>
 *   <li>{@code bufferFe} is the energy-cell cache only (IO / display / overflow).</li>
 *   <li>Overflow / push-out must never touch recipe progress.</li>
 * </ul>
 */
public final class OrbEnergyMath {

    private OrbEnergyMath() {
    }

    /** @return how much of {@code amount} is absorbed into recipe progress (rest is cache). */
    public static long fillRecipe(long recipeProgress, long recipeEnergy, long amount) {
        if (recipeEnergy <= 0 || amount <= 0) {
            return 0;
        }
        long need = Math.max(0L, recipeEnergy - recipeProgress);
        return Math.min(need, amount);
    }

    /**
     * Move stored cache energy into recipe progress (rods/cache drive craft).
     *
     * @return how much left the cache
     */
    public static long feedProgressFromBuffer(long recipeProgress, long recipeEnergy, long bufferFe) {
        if (recipeEnergy <= 0 || bufferFe <= 0) {
            return 0;
        }
        long need = Math.max(0L, recipeEnergy - recipeProgress);
        return Math.min(need, bufferFe);
    }

    /** Cache fill room (energy cell). Never limited by recipe progress. */
    public static long cacheRoom(long bufferFe, long cacheMax) {
        return Math.max(0L, cacheMax - bufferFe);
    }

    /**
     * Excess above the current cache cap only. Rods may raise the cap — do not
     * shed down to BASE when rods still provide room.
     */
    public static long overflowAbove(long bufferFe, long cacheMax) {
        return Math.max(0L, bufferFe - cacheMax);
    }

    /** True when recipe progress is ready to complete. */
    public static boolean ready(long recipeProgress, long recipeEnergy) {
        return recipeEnergy > 0 && recipeProgress >= recipeEnergy;
    }

    /**
     * Legacy NBT split: old saves kept progress inside {@code bufferFe}.
     * Allocate {@code min(bufferFe, recipeEnergy)} to progress when a recipe is loaded.
     *
     * @return array {recipeProgress, bufferFe} after migration
     */
    public static long[] migrateLegacyBuffer(long bufferFe, long recipeEnergy, boolean containRecipe) {
        if (!containRecipe || recipeEnergy <= 0 || bufferFe <= 0) {
            return new long[] {0L, Math.max(0L, bufferFe)};
        }
        long progress = Math.min(bufferFe, recipeEnergy);
        return new long[] {progress, bufferFe - progress};
    }
}
