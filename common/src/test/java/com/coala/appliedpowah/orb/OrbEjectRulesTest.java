package com.coala.appliedpowah.orb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OrbEjectRules} — the product-settlement decision table (0.6.2).
 *
 * <p>Encodes the ejection tri-state: OFF keeps everything inside the machine,
 * SIDES pushes only toward configured faces, NETWORK is the old auto-export ON
 * behaviour (ME storage + CPU returnInv).</p>
 */
class OrbEjectRulesTest {

    @Test
    @DisplayName("CPU returnInv only in NETWORK mode")
    void cpuReturnOnlyInNetworkMode() {
        for (EjectMode mode : EjectMode.values()) {
            assertEquals(mode == EjectMode.NETWORK,
                    OrbEjectRules.allowsCpuReturn(mode, true),
                    mode + " cpuReturn");
            assertFalse(OrbEjectRules.allowsCpuReturn(mode, false),
                    "manual inserts never go to a CPU return inventory");
        }
    }

    @Test
    @DisplayName("provider return needs a return side and any ejecting mode")
    void providerReturn() {
        for (EjectMode mode : EjectMode.values()) {
            assertEquals(mode.ejects(), OrbEjectRules.allowsProviderReturn(mode, true));
            assertFalse(OrbEjectRules.allowsProviderReturn(mode, false));
        }
    }

    @Test
    @DisplayName("side push only in SIDES mode and only for configured faces")
    void sidePush() {
        EnumSet<Direction> allowed = EnumSet.of(Direction.NORTH, Direction.DOWN);
        assertTrue(OrbEjectRules.allowsSidePush(EjectMode.SIDES, Direction.NORTH, allowed));
        assertTrue(OrbEjectRules.allowsSidePush(EjectMode.SIDES, Direction.DOWN, allowed));
        assertFalse(OrbEjectRules.allowsSidePush(EjectMode.SIDES, Direction.UP, allowed));
        assertFalse(OrbEjectRules.allowsSidePush(EjectMode.SIDES, Direction.NORTH, null));
        for (Direction side : Direction.values()) {
            assertFalse(OrbEjectRules.allowsSidePush(EjectMode.OFF, side, allowed));
            assertFalse(OrbEjectRules.allowsSidePush(EjectMode.NETWORK, side, allowed));
        }
    }

    @Test
    @DisplayName("network push only in NETWORK mode")
    void networkPush() {
        for (EjectMode mode : EjectMode.values()) {
            assertEquals(mode == EjectMode.NETWORK, OrbEjectRules.allowsNetworkPush(mode));
        }
    }

    @Test
    @DisplayName("ejectFaces is empty unless SIDES with a non-empty face set")
    void ejectFaces() {
        EnumSet<Direction> allowed = EnumSet.of(Direction.WEST);
        assertEquals(EnumSet.of(Direction.WEST),
                OrbEjectRules.ejectFaces(EjectMode.SIDES, allowed));
        assertTrue(OrbEjectRules.ejectFaces(EjectMode.SIDES, EnumSet.noneOf(Direction.class)).isEmpty());
        assertTrue(OrbEjectRules.ejectFaces(EjectMode.SIDES, null).isEmpty());
        for (EjectMode mode : new EjectMode[] {EjectMode.OFF, EjectMode.NETWORK}) {
            assertTrue(OrbEjectRules.ejectFaces(mode, allowed).isEmpty());
        }
    }

    @Test
    @DisplayName("eject mode cycles OFF → SIDES → NETWORK → OFF")
    void modeCycle() {
        assertEquals(EjectMode.SIDES, EjectMode.OFF.next());
        assertEquals(EjectMode.NETWORK, EjectMode.SIDES.next());
        assertEquals(EjectMode.OFF, EjectMode.NETWORK.next());
        assertFalse(EjectMode.OFF.ejects());
        assertTrue(EjectMode.SIDES.ejects());
        assertTrue(EjectMode.NETWORK.ejects());
    }
}
