package com.coala.appliedpowah.orb;

/**
 * Implemented by Applied Powah energizing orbs so charging rods can feed them
 * the same way they feed Powah's {@code EnergizingOrbTile}.
 */
public interface EnergyAcceptingOrb {
    boolean containRecipe();

    /** @return FE actually accepted */
    long fillEnergy(long amount);

    /**
     * Whether charging rods may beam into this orb. Only the basic (Fluix) orb
     * is rod-fed by design; Advanced / ME orbs carry rod slots / network pull
     * and must not be linkable (user spec, issues.md 0.6.0-2).
     */
    default boolean rodFeedable() {
        return true;
    }
}
