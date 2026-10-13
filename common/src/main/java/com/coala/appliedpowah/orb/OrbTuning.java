package com.coala.appliedpowah.orb;

/**
 * Pure tuning math for the multi-page orb rewrite (issues.md 其他-10).
 * No Minecraft types — unit-testable.
 *
 * <pre>
 * speed cards (AE2 {@code ae2:speed_card}, max 5):
 *   factor(cards) = {1, 2, 3, 5, 10, 25}
 *   stepEnergy    = E0 × factor × ticks / 100        (FE wanted per tick)
 *   → one completion always costs exactly E0, whatever the card count
 *     (AAE reaction-chamber model: energy per tick ∝ speed, total constant).
 * energy cards (AE2 {@code ae2:energy_card}, max 4) — supply bandwidth only:
 *   C     = C0 × (1.125)^n
 *   r_in  = 10% + 20%×n
 *   r_out = 1% + 2.25%×n
 * </pre>
 */
public final class OrbTuning {

    public static final int SPEED_CARD_MAX = 5;
    public static final int ENERGY_CARD_MAX = 4;
    public static final double CACHE_GROWTH_PER_CARD = 0.125;
    public static final double R_IN0 = 0.10;
    public static final double R_IN_STEP = 0.20;
    public static final double R_OUT0 = 0.01;
    public static final double R_OUT_STEP = 0.0225;

    /** 0..5 acceleration cards → progress factor (percent per tick at full power). */
    private static final int[] SPEED_FACTORS = {1, 2, 3, 5, 10, 25};

    private OrbTuning() {
    }

    /** Clamp card count into [0, SPEED_CARD_MAX]. */
    public static int clampSpeedCards(int cards) {
        return Math.max(0, Math.min(SPEED_CARD_MAX, cards));
    }

    public static int clampEnergyCards(int cards) {
        return Math.max(0, Math.min(ENERGY_CARD_MAX, cards));
    }

    /** Progress factor for an installed acceleration-card count (0 → 1). */
    public static int speedFactor(int cards) {
        return SPEED_FACTORS[clampSpeedCards(cards)];
    }

    /**
     * FE a page wants this tick to advance at full speed.
     * Capped callers reduce it to the remaining recipe need.
     */
    public static long stepEnergy(long e0, int speedCards, int ticks) {
        if (e0 <= 0 || ticks <= 0) {
            return 0;
        }
        double want = e0 * (double) speedFactor(speedCards) * ticks / 100.0;
        return Math.max(1L, Math.round(want));
    }

    /** C = C0 × (1.125)^n */
    public static long cacheMax(long c0, int energyCards) {
        int n = clampEnergyCards(energyCards);
        return Math.max(1L, Math.round(c0 * Math.pow(1.0 + CACHE_GROWTH_PER_CARD, n)));
    }

    /** Network pull fraction of C per charge step. */
    public static double rIn(int energyCards) {
        return R_IN0 + R_IN_STEP * clampEnergyCards(energyCards);
    }

    /** Output fraction of C per tick. */
    public static double rOut(int energyCards) {
        return R_OUT0 + R_OUT_STEP * clampEnergyCards(energyCards);
    }

    public static long rateLimit(long cacheMax, double fraction) {
        if (cacheMax <= 0 || fraction <= 0) {
            return 0;
        }
        return Math.max(1L, Math.round(cacheMax * fraction));
    }
}
