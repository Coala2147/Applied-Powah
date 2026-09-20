package com.coala.appliedpowah.energycell;

import appeng.block.networking.EnergyCellBlock;

/** Extreme-dense AE cell — 8 × super-dense = 102.4M AE (rates ×8 of super, pending user tweak). */
public class ExtremeDenseEnergyCellBlock extends EnergyCellBlock {
    public static final double MAX_POWER = 102_400_000D;
    public static final double CHARGE_RATE = 102_400D;
    public static final int PRIORITY = 12_800;

    public ExtremeDenseEnergyCellBlock() {
        super(MAX_POWER, CHARGE_RATE, PRIORITY);
    }
}
