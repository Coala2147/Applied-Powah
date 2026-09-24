package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Family caps for the 6-slot upgrade inventory (logic-only via counts). */
class UpgradeCardInvTest {

    // Item identity is Minecraft-bound; exercise the pure count/cap rules via a stub.

    @Test
    void parallelSnapsFromInstalledValue() {
        assertEquals(1, OrbTuning.parallelOf(1));
        assertEquals(2, OrbTuning.parallelOf(2));
        assertEquals(4, OrbTuning.parallelOf(4));
        assertEquals(6, OrbTuning.parallelOf(6));
        assertEquals(8, OrbTuning.parallelOf(8));
    }

    @Test
    void threadCapMatchesCardLadder() {
        assertEquals(1, OrbTuning.threadCap(0));
        assertEquals(2, OrbTuning.threadCap(1));
        assertEquals(3, OrbTuning.threadCap(2));
        assertEquals(5, OrbTuning.threadCap(4));
        assertEquals(9, OrbTuning.threadCap(8));
    }

    @Test
    void energyCardMaxIs4() {
        assertEquals(4, UpgradeCardInv.ENERGY_MAX);
        assertEquals(6, UpgradeCardInv.SLOTS);
    }

    @Test
    void readyGateUsesProgressNotBuffer() {
        assertTrue(OrbEnergyMath.ready(304, 304));
        assertFalse(OrbEnergyMath.ready(303, 304));
    }
}
