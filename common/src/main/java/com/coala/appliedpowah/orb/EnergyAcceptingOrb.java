package com.coala.appliedpowah.orb;

/**
 * Implemented by Applied Powah energizing orbs so charging rods can feed them
 * the same way they feed Powah's {@code EnergizingOrbTile}.
 */
public interface EnergyAcceptingOrb {
    boolean containRecipe();

    /** @return FE actually accepted */
    long fillEnergy(long amount);
}
