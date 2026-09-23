package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A-R1: pick nearest orb, not scan-order first; remembered only if still nearest. */
class NearestPosTest {

    @Test
    void picksNearestNotFirst() {
        // far first in array (old betweenClosed order would pick it)
        long[] xs = {10, 1, 5};
        long[] ys = {0, 0, 0};
        long[] zs = {0, 0, 0};
        assertEquals(1, NearestPos.nearestIndex(xs, ys, zs, 0, 0, 0, -1));
    }

    @Test
    void rememberedWinsOnTie() {
        long[] xs = {2, 0, -2};
        long[] ys = {0, 2, 0};
        long[] zs = {0, 0, 0};
        // dist to origin: all 2 (squared 4). remembered index 0
        assertEquals(0, NearestPos.nearestIndex(xs, ys, zs, 0, 0, 0, 0));
    }

    @Test
    void rememberedLosesWhenFarther() {
        long[] xs = {10, 1};
        long[] ys = {0, 0};
        long[] zs = {0, 0};
        assertEquals(1, NearestPos.nearestIndex(xs, ys, zs, 0, 0, 0, 0));
    }

    @Test
    void emptyReturnsNone() {
        assertEquals(-1, NearestPos.nearestIndex(new long[0], new long[0], new long[0], 0, 0, 0, -1));
    }

    @Test
    void distSqMatches() {
        assertEquals(14, NearestPos.distSq(1, 2, 3, 0, 0, 0));
        assertTrue(NearestPos.distSq(0, 0, 0, 3, 4, 0) == 25);
    }

    @Test
    void selectStableDelegates() {
        long[] xs = {100, 1};
        long[] ys = {0, 0};
        long[] zs = {0, 0};
        assertEquals(1, NearestPos.selectStable(xs, ys, zs, 0, 0, 0, 0));
    }
}
