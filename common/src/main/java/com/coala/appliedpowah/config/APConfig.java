package com.coala.appliedpowah.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class APConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        var builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    private APConfig() {
    }

    public static void register() {
        // Custom file name: applied_powah.toml (no -common suffix).
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "applied_powah.toml");
    }

    public static final class Common {
        public final ForgeConfigSpec.IntValue pullIntervalTicks;
        public final ForgeConfigSpec.DoubleValue networkReserveRatio;
        public final ForgeConfigSpec.LongValue aeBurstAe;
        public final ForgeConfigSpec.LongValue meBurstFe;
        public final ForgeConfigSpec.BooleanValue rodsKeepEnergyOnBreak;
        public final ForgeConfigSpec.BooleanValue orbMultiFacing;

        Common(ForgeConfigSpec.Builder b) {
            b.push("charging_rod");
            pullIntervalTicks = b
                    .comment("Ticks between network energy bursts into rod buffer")
                    .defineInRange("pullIntervalTicks", 1, 1, 1200);
            networkReserveRatio = b
                    .comment("Fraction of AE network max energy that must remain after extraction (0.05 = 5%)")
                    .defineInRange("networkReserveRatio", 0.05D, 0.0D, 0.95D);
            aeBurstAe = b
                    .comment("AE extracted per burst attempt (AE rod). FE equivalent is roughly AE x 2.")
                    .defineInRange("aeBurstAe", 10_000_000L, 1L, Long.MAX_VALUE / 4);
            meBurstFe = b
                    .comment("FE extracted per burst attempt (ME rod, Applied Flux)")
                    .defineInRange("meBurstFe", 20_000_000L, 1L, Long.MAX_VALUE / 4);
            rodsKeepEnergyOnBreak = b
                    .comment("Powah-style drops: mined/unplaced rods keep their FE buffer on the item NBT")
                    .define("rodsKeepEnergyOnBreak", true);
            b.pop();

            b.push("energizing_orb");
            orbMultiFacing = b
                    .comment("If true, AP energizing orbs may connect to the ME network on more than the bottom face "
                            + "(rod-like multi-face). Default false = bottom-only (ExtendedAE Caner-style).")
                    .define("orbMultiFacing", false);
            b.pop();
        }
    }
}
