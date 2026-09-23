package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R1 stacking contract: empty place, grow, refuse full / different. */
class OrbResultRulesTest {

    @Test
    void emptySlotAccepts() {
        assertTrue(OrbResultRules.canPlace(0, false, 1));
        assertEquals(1, OrbResultRules.afterPlace(0, false, 1));
    }

    @Test
    void sameItemGrows() {
        assertTrue(OrbResultRules.canPlace(1, true, 1));
        assertEquals(2, OrbResultRules.afterPlace(1, true, 1));
        assertEquals(3, OrbResultRules.afterPlace(2, true, 1));
    }

    @Test
    void fullSlotRefusesWithoutEating() {
        assertFalse(OrbResultRules.canPlace(64, true, 1));
        assertEquals(-1, OrbResultRules.afterPlace(64, true, 1));
        assertFalse(OrbResultRules.canPlace(63, true, 2));
    }

    @Test
    void differentItemRefuses() {
        assertFalse(OrbResultRules.canPlace(1, false, 1));
        assertEquals(-1, OrbResultRules.afterPlace(5, false, 1));
    }

    @Test
    void invalidAddCountRefuses() {
        assertFalse(OrbResultRules.canPlace(0, false, 0));
        assertFalse(OrbResultRules.canPlace(0, false, 65));
        assertFalse(OrbResultRules.canPlace(1, true, -1));
    }

    @Test
    void growExactlyTo64() {
        assertTrue(OrbResultRules.canPlace(63, true, 1));
        assertEquals(64, OrbResultRules.afterPlace(63, true, 1));
    }

    @Test
    void settleAllowsFullReturnSinkWithoutOutput() {
        // regression: empty remainder after provider/CPU return must not refuse the craft
        assertTrue(OrbResultRules.canSettle(true, false));
        assertTrue(OrbResultRules.canSettle(true, true));
        assertTrue(OrbResultRules.canSettle(false, true));
        assertFalse(OrbResultRules.canSettle(false, false));
    }
}
