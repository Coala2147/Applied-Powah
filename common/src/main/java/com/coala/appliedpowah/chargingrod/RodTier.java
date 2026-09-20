package com.coala.appliedpowah.chargingrod;

/** Powah-aligned tier defaults for AP energizing rods. */
public enum RodTier {
    STARTER("starter", 10_000L, 100L),
    BASIC("basic", 40_000L, 400L),
    HARDENED("hardened", 100_000L, 1_000L),
    BLAZING("blazing", 400_000L, 4_000L),
    NIOTIC("niotic", 1_000_000L, 10_000L),
    SPIRITED("spirited", 4_000_000L, 40_000L),
    NITRO("nitro", 20_000_000L, 200_000L);

    public final String id;
    /** Internal FE buffer capacity (Powah energizing_rods.capacity). */
    public final long capacityFe;
    /** Output push limit to Energizing Orb (Powah transfer). */
    public final long transferFe;

    RodTier(String id, long capacityFe, long transferFe) {
        this.id = id;
        this.capacityFe = capacityFe;
        this.transferFe = transferFe;
    }

    public String aeRodId() {
        return "ae_energizing_rod_" + id;
    }

    public String meRodId() {
        return "me_energizing_rod_" + id;
    }

    public String powahRodId() {
        return "energizing_rod_" + id;
    }
}
