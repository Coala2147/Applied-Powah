package com.coala.appliedpowah.orb;

/**
 * T/P energy model (开发总报告 §2.3). Pure math — unit-testable.
 *
 * <pre>
 * t_cap = 1 + Σ(thread_card) ∈ {1,2,3,5,9}
 * P     ∈ {1,2,4,6,8}
 * n     = Σ(energy_card) ≤ 4
 * waste = 0.90 × (t_act / 9)
 * E_need = E0 × P × (1 + waste)
 * C     = C0 × (1.125)^n
 * r_in  = 10% + 20%×n
 * r_out = 1% + 2.25%×n
 * </pre>
 */
public final class OrbTuning {

    public static final double WASTE_MAX = 0.90;
    public static final int THREAD_CAP = 9;
    public static final double CACHE_GROWTH_PER_CARD = 0.125;
    public static final double R_IN0 = 0.10;
    public static final double R_IN_STEP = 0.20;
    public static final double R_OUT0 = 0.01;
    public static final double R_OUT_STEP = 0.0225;
    public static final int ENERGY_CARD_MAX = 4;
    public static final int PARALLEL_LEVELS = 4;

    private OrbTuning() {
    }

    /** t_cap = 1 + bonus; bonus = Σ thread-card values (1/2/4/8), capped at 9. */
    public static int threadCap(int threadBonus) {
        return Math.max(1, Math.min(THREAD_CAP, 1 + Math.max(0, threadBonus)));
    }

    /** Clamp parallel multiplier to {1,2,4,6,8} — take the max valid installed card. */
    public static int parallelOf(int raw) {
        if (raw >= 8) {
            return 8;
        }
        if (raw >= 6) {
            return 6;
        }
        if (raw >= 4) {
            return 4;
        }
        if (raw >= 2) {
            return 2;
        }
        return 1;
    }

    public static double waste(int tAct) {
        int t = Math.max(0, Math.min(THREAD_CAP, tAct));
        return WASTE_MAX * t / THREAD_CAP;
    }

    /** E_need = E0 × P × (1 + waste). At least 1. */
    public static long eNeed(long e0, int p, int tAct) {
        if (e0 <= 0) {
            return 0;
        }
        double mult = Math.max(1, p) * (1.0 + waste(tAct));
        return Math.max(1L, Math.round(e0 * mult));
    }

    /** C = C0 × (1.125)^n */
    public static long cacheMax(long c0, int energyCards) {
        int n = Math.max(0, Math.min(ENERGY_CARD_MAX, energyCards));
        return Math.max(1L, Math.round(c0 * Math.pow(1.0 + CACHE_GROWTH_PER_CARD, n)));
    }

    /** Network pull fraction of C per charge step. */
    public static double rIn(int energyCards) {
        int n = Math.max(0, Math.min(ENERGY_CARD_MAX, energyCards));
        return R_IN0 + R_IN_STEP * n;
    }

    /** Output fraction of C per tick. */
    public static double rOut(int energyCards) {
        int n = Math.max(0, Math.min(ENERGY_CARD_MAX, energyCards));
        return R_OUT0 + R_OUT_STEP * n;
    }

    public static long rateLimit(long cacheMax, double fraction) {
        if (cacheMax <= 0 || fraction <= 0) {
            return 0;
        }
        return Math.max(1L, Math.round(cacheMax * fraction));
    }
}
