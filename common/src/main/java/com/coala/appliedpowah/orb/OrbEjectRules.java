package com.coala.appliedpowah.orb;

import java.util.EnumSet;
import net.minecraft.core.Direction;

/**
 * Pure product-settlement decision rules for the multi-page orbs.
 * No inventories — unit-testable. The BE stays a thin executor of these rules.
 *
 * <p>Settlement order per completed page (first sink that accepts wins):</p>
 * <ol>
 *   <li>CPU returnInv → network (NETWORK mode + order came from a CPU/provider push);</li>
 *   <li>provider return (any eject mode except OFF — back to the pusher side);</li>
 *   <li>page OUTPUT slot (empty / same-stack grow / refuse-and-wait).</li>
 * </ol>
 */
public final class OrbEjectRules {

    private OrbEjectRules() {
    }

    /**
     * Whether a page whose inputs came from a CPU order may settle through the
     * pattern-provider returnInv. Keeps the 0.4.9 rule "return-to-network and
     * auto-export are the same switch": only NETWORK mode injects to the network.
     */
    public static boolean allowsCpuReturn(EjectMode mode, boolean fromCpuOrder) {
        return mode == EjectMode.NETWORK && fromCpuOrder;
    }

    /** Whether a finished stack may be pushed back to the side that pushed the inputs. */
    public static boolean allowsProviderReturn(EjectMode mode, boolean hasReturnSide) {
        return mode.ejects() && hasReturnSide;
    }

    /** Whether a finished stack may be ejected toward a specific neighbour face. */
    public static boolean allowsSidePush(EjectMode mode, Direction side, EnumSet<Direction> allowed) {
        return mode == EjectMode.SIDES && allowed != null && allowed.contains(side);
    }

    /** Network injection of page outputs (auto-export ON equivalent). */
    public static boolean allowsNetworkPush(EjectMode mode) {
        return mode == EjectMode.NETWORK;
    }

    /** Which faces the side-eject pass should visit (empty when not in SIDES mode). */
    public static EnumSet<Direction> ejectFaces(EjectMode mode, EnumSet<Direction> allowed) {
        if (mode != EjectMode.SIDES || allowed == null || allowed.isEmpty()) {
            return EnumSet.noneOf(Direction.class);
        }
        return EnumSet.copyOf(allowed);
    }
}
