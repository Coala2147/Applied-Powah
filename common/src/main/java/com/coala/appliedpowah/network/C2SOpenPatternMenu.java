package com.coala.appliedpowah.network;

import com.coala.appliedpowah.orb.AutoEnergizingOrbBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: open Pattern Provider menu for Auto Energizing Orb. */
public record C2SOpenPatternMenu(BlockPos pos) {

    public static void encode(C2SOpenPatternMenu msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static C2SOpenPatternMenu decode(FriendlyByteBuf buf) {
        return new C2SOpenPatternMenu(buf.readBlockPos());
    }

    public static void handle(C2SOpenPatternMenu msg, Supplier<NetworkEvent.Context> ctx) {
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
            if (be instanceof AutoEnergizingOrbBlockEntity auto) {
                auto.openPatternMenu(player, appeng.menu.locator.MenuLocators.forBlockEntity(auto));
            }
        });
        context.setPacketHandled(true);
    }
}
