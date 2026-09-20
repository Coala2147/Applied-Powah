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
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    public static final class Common {
        public final ForgeConfigSpec.IntValue pullIntervalTicks;
        public final ForgeConfigSpec.DoubleValue networkReserveRatio;
        public final ForgeConfigSpec.LongValue aeBurstAe;
        public final ForgeConfigSpec.LongValue meBurstFe;

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
            b.pop();
        }
    }
}
