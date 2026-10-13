package com.coala.appliedpowah.orb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OrbTuning} contract of the multi-page orb (0.6.2).
 *
 * <ul>
 *   <li>Acceleration cards: total energy per completion is constant, only the
 *       tick cost scales (AAE reaction-chamber model);</li>
 *   <li>energy cards: cache growth (×1.125^n) and bandwidth (r_in / r_out) only —
 *       they never touch recipe speed;</li>
 *   <li>card counts are clamped to the AE2-registered caps.</li>
 * </ul>
 */
class OrbTuningTest {

    @Test
    @DisplayName("speed factors: 1/2/3/5/10/25 for 0..5 cards")
    void speed_factors() {
        assertEquals(1, OrbTuning.speedFactor(0));
        assertEquals(2, OrbTuning.speedFactor(1));
        assertEquals(3, OrbTuning.speedFactor(2));
        assertEquals(5, OrbTuning.speedFactor(3));
        assertEquals(10, OrbTuning.speedFactor(4));
        assertEquals(25, OrbTuning.speedFactor(5));
        // clamped at the AE2 registration cap
        assertEquals(25, OrbTuning.speedFactor(6));
        assertEquals(25, OrbTuning.speedFactor(99));
        assertEquals(1, OrbTuning.speedFactor(-1));
    }

    @Test
    @DisplayName("stepEnergy: one completion always costs exactly E0")
    void step_energy_total_cost() {
        for (int cards = 0; cards <= 5; cards++) {
            long e0 = 100_000L;
            long step = OrbTuning.stepEnergy(e0, cards, 20);
            int factor = OrbTuning.speedFactor(cards);
            assertTrue(step > 0, "step must stay positive");
            // the formula is exactly E0 * factor * ticks / 100 (callers cap at the remainder)
            long want = Math.max(1L, Math.round(e0 * factor * 20 / 100.0));
            assertEquals(want, step);
            // simulate the feeder capping at the remaining energy
            long remaining = e0;
            long total = 0;
            while (remaining > 0) {
                long take = Math.min(step, remaining);
                total += take;
                remaining -= take;
            }
            assertEquals(e0, total, "total must equal E0 exactly (no waste, no gain)");
        }
    }

    @Test
    @DisplayName("stepEnergy guards")
    void step_energy_guards() {
        assertEquals(0, OrbTuning.stepEnergy(0, 3, 20));
        assertEquals(0, OrbTuning.stepEnergy(1000, 0, 0));
        assertEquals(0, OrbTuning.stepEnergy(-5, 1, 10));
    }

    @Test
    @DisplayName("cache grows ×1.125 per energy card, clamped to 4")
    void cache_growth() {
        long c0 = 800_000L;
        assertEquals(c0, OrbTuning.cacheMax(c0, 0));
        assertEquals(Math.round(c0 * 1.125), OrbTuning.cacheMax(c0, 1));
        assertEquals(Math.round(c0 * Math.pow(1.125, 4)), OrbTuning.cacheMax(c0, 4));
        assertEquals(Math.round(c0 * Math.pow(1.125, 4)), OrbTuning.cacheMax(c0, 9));
    }

    @Test
    @DisplayName("r_in / r_out only grow with energy cards")
    void bandwidth() {
        assertEquals(0.10, OrbTuning.rIn(0), 1e-9);
        assertEquals(0.30, OrbTuning.rIn(1), 1e-9);
        assertEquals(0.90, OrbTuning.rIn(4), 1e-9);
        assertEquals(0.90, OrbTuning.rIn(9), 1e-9, "clamped");

        assertEquals(0.01, OrbTuning.rOut(0), 1e-9);
        assertEquals(0.10, OrbTuning.rOut(4), 1e-9);

        assertEquals(80_000L, OrbTuning.rateLimit(800_000L, 0.10));
        assertEquals(0, OrbTuning.rateLimit(0, 0.10));
        assertEquals(0, OrbTuning.rateLimit(800_000L, 0.0));
    }

    @Test
    @DisplayName("card caps match the AE2 registrations")
    void card_caps() {
        assertEquals(5, OrbTuning.SPEED_CARD_MAX);
        assertEquals(4, OrbTuning.ENERGY_CARD_MAX);
    }
}
