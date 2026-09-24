package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §2.3 T/P / waste / C(n) / r_in / r_out. */
class OrbTuningTest {

    @Test
    void threadCapIs1PlusBonusCappedAt9() {
        assertEquals(1, OrbTuning.threadCap(0));
        assertEquals(2, OrbTuning.threadCap(1));
        assertEquals(5, OrbTuning.threadCap(4));
        assertEquals(9, OrbTuning.threadCap(8));
        assertEquals(9, OrbTuning.threadCap(99));
    }

    @Test
    void parallelSnapsToAllowedSet() {
        assertEquals(1, OrbTuning.parallelOf(1));
        assertEquals(2, OrbTuning.parallelOf(2));
        assertEquals(2, OrbTuning.parallelOf(3));
        assertEquals(4, OrbTuning.parallelOf(4));
        assertEquals(6, OrbTuning.parallelOf(6));
        assertEquals(8, OrbTuning.parallelOf(8));
        assertEquals(8, OrbTuning.parallelOf(16));
    }

    @Test
    void wasteScalesWithActiveThreads() {
        assertEquals(0.0, OrbTuning.waste(0), 1e-9);
        assertEquals(0.10, OrbTuning.waste(1), 1e-9);
        assertEquals(0.90, OrbTuning.waste(9), 1e-9);
    }

    @Test
    void eNeedIsE0TimesPAndWaste() {
        assertEquals(20_000_000L, OrbTuning.eNeed(20_000_000L, 1, 0));
        // P=8, tAct=0 → 160M
        assertEquals(160_000_000L, OrbTuning.eNeed(20_000_000L, 8, 0));
        // P=8, tAct=9 → 160M × 1.9 = 304M
        assertEquals(304_000_000L, OrbTuning.eNeed(20_000_000L, 8, 9));
    }

    @Test
    void cacheGrowsCompoundingPerEnergyCard() {
        long c0 = 204_800_000L;
        assertEquals(c0, OrbTuning.cacheMax(c0, 0));
        long c4 = OrbTuning.cacheMax(c0, 4);
        // (1.125)^4 ≈ 1.6018
        assertEquals(Math.round(c0 * Math.pow(1.125, 4)), c4);
        assertTrue(c4 > c0);
    }

    @Test
    void ratesGoFrom10to90And1to10() {
        assertEquals(0.10, OrbTuning.rIn(0), 1e-9);
        assertEquals(0.90, OrbTuning.rIn(4), 1e-9);
        assertEquals(0.01, OrbTuning.rOut(0), 1e-9);
        assertEquals(0.10, OrbTuning.rOut(4), 1e-9);
    }
}
