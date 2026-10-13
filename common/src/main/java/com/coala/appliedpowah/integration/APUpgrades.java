package com.coala.appliedpowah.integration;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.orb.APOrbs;

/**
 * Registers AE2's own upgrade cards for the three orb tiers.
 *
 * <ul>
 *   <li>every tier: {@code ae2:speed_card} ×5 (Acceleration) — the multi-page speed
 *       model of {@link com.coala.appliedpowah.orb.OrbTuning};</li>
 *   <li>Advanced / ME: {@code ae2:energy_card} ×4 — supply bandwidth only (cache size +
 *       network IO). Fluix has neither cache nor network pull, so energy cards are not
 *       registered for it.</li>
 * </ul>
 */
public final class APUpgrades {

    /** Fluix tier: acceleration only (no cache, no grid pull). */
    public static final int SPEED_CARDS = 5;
    /** Advanced / ME tiers. */
    public static final int ENERGY_CARDS = 4;

    private APUpgrades() {
    }

    /** Called from {@link Ae2Bridge} on the common setup work queue. */
    public static void register() {
        try {
            Upgrades.add(AEItems.SPEED_CARD, APOrbs.FLUIX_ORB.get(), SPEED_CARDS);
            Upgrades.add(AEItems.SPEED_CARD, APOrbs.ADV_ORB.get(), SPEED_CARDS);
            Upgrades.add(AEItems.SPEED_CARD, APOrbs.ME_ORB.get(), SPEED_CARDS);
            Upgrades.add(AEItems.ENERGY_CARD, APOrbs.ADV_ORB.get(), ENERGY_CARDS);
            Upgrades.add(AEItems.ENERGY_CARD, APOrbs.ME_ORB.get(), ENERGY_CARDS);
            AppliedPowah.LOG.info("Registered AE2 upgrade cards (speed x{}, energy x{}) for orbs",
                    SPEED_CARDS, ENERGY_CARDS);
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("Upgrade card registration failed: {}", t.toString());
        }
    }
}
