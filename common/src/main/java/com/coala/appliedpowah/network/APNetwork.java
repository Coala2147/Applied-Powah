package com.coala.appliedpowah.network;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** Forge SimpleChannel for AP orb GUI actions. */
public final class APNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AppliedPowah.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static boolean registered;

    private APNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, C2SToggleAutoExport.class,
                C2SToggleAutoExport::encode,
                C2SToggleAutoExport::decode,
                C2SToggleAutoExport::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        AppliedPowah.LOG.info("Registered AP network channel");
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
