package com.coala.appliedpowah.energycell;

import appeng.block.networking.EnergyCellBlock;

/** Super-dense AE cell — 8 × dense (1.6M) = 12.8M AE. */
public class SuperDenseEnergyCellBlock extends EnergyCellBlock {
    public static final double MAX_POWER = 12_800_000D;
    public static final double CHARGE_RATE = 12_800D;
    public static final int PRIORITY = 1600;

    public SuperDenseEnergyCellBlock() {
        super(MAX_POWER, CHARGE_RATE, PRIORITY);
    }
}
