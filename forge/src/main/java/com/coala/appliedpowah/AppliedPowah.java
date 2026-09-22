package com.coala.appliedpowah;

import com.coala.appliedpowah.chargingrod.RodTier;
import com.coala.appliedpowah.config.APConfig;
import com.coala.appliedpowah.energycell.APCells;
import com.coala.appliedpowah.integration.Ae2Bridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AppliedPowah.MOD_ID)
public final class AppliedPowah {
    public static final String MOD_ID = "applied_powah";
    public static final Logger LOG = LogManager.getLogger("AppliedPowah");

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.applied_powah"))
                    .icon(() -> {
                        try {
                            var rod = APCells.AE_RODS.get(RodTier.STARTER);
                            if (rod != null && rod.isPresent()) {
                                return new ItemStack(rod.get());
                            }
                        } catch (Throwable ignored) {
                        }
                        try {
                            return new ItemStack(APCells.SUPER_DENSE_ITEM.get());
                        } catch (Throwable t) {
                            return ItemStack.EMPTY;
                        }
                    })
                    .displayItems((params, output) -> {
                        try {
                            output.accept(APCells.SUPER_DENSE_ITEM.get());
                            output.accept(APCells.EXTREME_DENSE_ITEM.get());
                        } catch (Throwable t) {
                            LOG.warn("Creative tab cells: {}", t.toString());
                        }
                        try {
                            for (var entry : APCells.AE_RODS.values()) {
                                output.accept(entry.get());
                            }
                        } catch (Throwable t) {
                            LOG.warn("Creative tab AE rods: {}", t.toString());
                        }
                        try {
                            for (var entry : APCells.ME_RODS.values()) {
                                output.accept(entry.get());
                            }
                        } catch (Throwable t) {
                            LOG.warn("Creative tab ME rods: {}", t.toString());
                        }
                        try {
                            if (com.coala.appliedpowah.orb.APOrbs.ME_ORB_ITEM.isPresent()) {
                                output.accept(com.coala.appliedpowah.orb.APOrbs.ME_ORB_ITEM.get());
                            }
                            if (com.coala.appliedpowah.orb.APOrbs.ADV_ORB_ITEM.isPresent()) {
                                output.accept(com.coala.appliedpowah.orb.APOrbs.ADV_ORB_ITEM.get());
                            }
                            if (com.coala.appliedpowah.orb.APOrbs.AUTO_ORB_ITEM.isPresent()) {
                                output.accept(com.coala.appliedpowah.orb.APOrbs.AUTO_ORB_ITEM.get());
                            }
                        } catch (Throwable t) {
                            // powah missing — orbs unregistered
                        }
                    })
                    .build());

    public AppliedPowah() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        APConfig.register();
        TABS.register(bus);
        bus.addListener(AppliedPowah::onClientSetup);

        boolean ae2 = isLoaded("ae2");
        boolean powah = isLoaded("powah");
        boolean appflux = isLoaded("appflux");
        LOG.info("Applied Powah loading — ae2={}, powah={}, appflux={}", ae2, powah, appflux);

        if (ae2) {
            Ae2Bridge.init(bus, powah, appflux);
        } else {
            LOG.error("AE2 not present — nothing will register.");
        }
        com.coala.appliedpowah.network.APNetwork.register();
    }

    private static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                        APCells.ROD_TYPE.get(),
                        com.coala.appliedpowah.client.EnergizingRodRenderer::new);
                LOG.info("Registered energizing rod beam renderer");
            } catch (Throwable t) {
                LOG.warn("Rod renderer not registered: {}", t.toString());
            }
            try {
                if (net.minecraftforge.fml.ModList.get().isLoaded("powah")
                        && com.coala.appliedpowah.orb.APOrbs.ORB_MENU != null) {
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            com.coala.appliedpowah.orb.APOrbs.ORB_MENU.get(),
                            com.coala.appliedpowah.orb.EnergizingOrbScreen::new);
                    LOG.info("Registered energizing orb screen");
                }
            } catch (Throwable t) {
                LOG.warn("Orb screen not registered: {}", t.toString());
            }
            try {
                // Mirrors ExtendedAE: InitScreens ties MenuType to a ScreenStyle JSON.
                appeng.init.client.InitScreens.register(
                        com.coala.appliedpowah.orb.AutoOrbPatternMenu.TYPE,
                        com.coala.appliedpowah.orb.AutoOrbPatternScreen::new,
                        "/screens/auto_orb_patterns.json");
                LOG.info("Registered auto orb pattern screen (4x9 Ex-Provider layout)");
            } catch (Throwable t) {
                LOG.warn("Auto orb pattern screen not registered: {}", t.toString());
            }
        });
    }

    private static boolean isLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }
}
