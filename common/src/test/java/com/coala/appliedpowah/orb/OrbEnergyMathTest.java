package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §1.1 progress/cache split: progressive fill, overflow never eats progress. */
class OrbEnergyMathTest {

    @Test
    void fillRecipeTowardNeedNotCache() {
        // recipe 304M can exceed a 0 cache room — progress is unbounded by cache
        assertEquals(304L, OrbEnergyMath.fillRecipe(0L, 304L, 1000L));
        assertEquals(4L, OrbEnergyMath.fillRecipe(300L, 304L, 1000L));
        assertEquals(0L, OrbEnergyMath.fillRecipe(304L, 304L, 1000L));
        assertEquals(0L, OrbEnergyMath.fillRecipe(0L, 0L, 1000L));
    }

    @Test
    void bufferFeedsProgressWhenRecipeActive() {
        assertEquals(50L, OrbEnergyMath.feedProgressFromBuffer(0L, 100L, 50L));
        assertEquals(20L, OrbEnergyMath.feedProgressFromBuffer(80L, 100L, 50L));
        assertEquals(0L, OrbEnergyMath.feedProgressFromBuffer(100L, 100L, 50L));
    }

    @Test
    void cacheRoomIgnoresRecipeProgress() {
        assertEquals(0L, OrbEnergyMath.cacheRoom(100L, 100L));
        assertEquals(25L, OrbEnergyMath.cacheRoom(75L, 100L));
    }

    @Test
    void overflowOnlyAboveCurrentCap() {
        // rods still present: cap=200, stored 180 → no shed toward BASE=100
        assertEquals(0L, OrbEnergyMath.overflowAbove(180L, 200L));
        assertEquals(30L, OrbEnergyMath.overflowAbove(230L, 200L));
    }

    @Test
    void readyRequiresFullProgress() {
        assertTrue(OrbEnergyMath.ready(304L, 304L));
        assertFalse(OrbEnergyMath.ready(303L, 304L));
        assertFalse(OrbEnergyMath.ready(0L, 0L));
    }

    @Test
    void migrateLegacySplitsBuffer() {
        assertArrayEquals(new long[] {0L, 50L}, OrbEnergyMath.migrateLegacyBuffer(50L, 100L, false));
        assertArrayEquals(new long[] {80L, 20L}, OrbEnergyMath.migrateLegacyBuffer(100L, 80L, true));
        assertArrayEquals(new long[] {40L, 0L}, OrbEnergyMath.migrateLegacyBuffer(40L, 80L, true));
    }
}
