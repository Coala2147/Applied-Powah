package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lane fill / ready / batch (§3.6). */
class CraftLaneTest {

    @Test
    void fillStopsAtNeedAndReady() {
        CraftLane lane = new CraftLane();
        lane.start(java.util.List.of(), 304L, 8, "nitro");
        assertEquals(100L, lane.fill(100L));
        assertEquals(100L, lane.recipeProgress);
        assertFalse(lane.ready());
        assertEquals(204L, lane.fill(1000L));
        assertTrue(lane.ready());
        assertEquals(0L, lane.fill(10L));
    }

    @Test
    void batchAndPercent() {
        CraftLane lane = new CraftLane();
        lane.start(java.util.List.of(), 100L, 4, "x");
        assertEquals(4, lane.batch);
        lane.fill(50L);
        assertEquals(50, lane.progressPercent());
        lane.clear();
        assertFalse(lane.active);
        assertEquals(0, lane.progressPercent());
    }
}
