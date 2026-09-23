package com.coala.appliedpowah.network;

import com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: leave ME pattern page and reopen the ME Energizing Orb GUI. */
public record C2SOpenOrbMenu(BlockPos pos) {

    public static void encode(C2SOpenOrbMenu msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static C2SOpenOrbMenu decode(FriendlyByteBuf buf) {
        return new C2SOpenOrbMenu(buf.readBlockPos());
    }

    public static void handle(C2SOpenOrbMenu msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.level() == null) {
                return;
            }
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (be instanceof MeEnergizingOrbBlockEntity meOrb) {
                meOrb.openOrbMenu(player);
            }
        });
        context.setPacketHandled(true);
    }
}
