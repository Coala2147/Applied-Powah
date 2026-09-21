package com.coala.appliedpowah.integration;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.energycell.APCells;
import com.coala.appliedpowah.orb.APOrbs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * AE2-dependent registration. Only loaded when AE2 is present.
 * Rods are full blocks (not cable-bus parts). Orbs require Powah (soft).
 */
public final class Ae2Bridge {
    private Ae2Bridge() {
    }

    public static void init(IEventBus bus, boolean powah, boolean appflux) {
        APCells.register(bus, appflux);
        if (powah) {
            APOrbs.register(bus);
        } else {
            AppliedPowah.LOG.warn("Powah missing — energizing orbs not registered");
        }
        AppliedPowah.LOG.info("AE2 present — cells+rods (powah={}, appflux={})", powah, appflux);
        bus.addListener(Ae2Bridge::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AppliedPowah.LOG.info("--- Applied Powah registered content ---");
            ForgeRegistries.BLOCKS.getEntries().stream()
                    .filter(e -> e.getKey().location().getNamespace().equals(AppliedPowah.MOD_ID))
                    .forEach(e -> AppliedPowah.LOG.info("  block {}", e.getKey()));
            ForgeRegistries.ITEMS.getEntries().stream()
                    .filter(e -> e.getKey().location().getNamespace().equals(AppliedPowah.MOD_ID))
                    .forEach(e -> AppliedPowah.LOG.info("  item {}", e.getKey()));
        });
    }
}
