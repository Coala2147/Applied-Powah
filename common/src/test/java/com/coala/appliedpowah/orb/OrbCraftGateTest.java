package com.coala.appliedpowah.orb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B-R1/B-R2: acceptsPlans + all-or-nothing full set. */
class OrbCraftGateTest {

    @Test
    void emptyMachineAccepts() {
        assertTrue(OrbCraftGate.acceptsPlans(false, false, 6));
    }

    @Test
    void halfSetContinues() {
        // 1 of 3 ingredients present → emptyInputSlots=5, not a matched recipe
        assertTrue(OrbCraftGate.acceptsPlans(false, false, 5));
        assertTrue(OrbCraftGate.canAcceptFullSet(5, 2));
    }

    @Test
    void fullSetStops() {
        // inputs already match a recipe (containRecipe) → do not accept another set
        assertFalse(OrbCraftGate.acceptsPlans(false, true, 5));
    }

    @Test
    void allSlotsFullStops() {
        assertFalse(OrbCraftGate.acceptsPlans(false, false, 0));
    }

    @Test
    void completingStops() {
        assertFalse(OrbCraftGate.acceptsPlans(true, false, 6));
    }

    @Test
    void fullSetNeedsEnoughSlots() {
        assertTrue(OrbCraftGate.canAcceptFullSet(3, 3));
        assertFalse(OrbCraftGate.canAcceptFullSet(2, 3));
        assertFalse(OrbCraftGate.canAcceptFullSet(3, 0));
    }
}
