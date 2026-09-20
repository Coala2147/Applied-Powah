package com.coala.appliedpowah.integration.powah;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import owmii.powah.Powah;
import owmii.powah.block.energizing.EnergizingOrbTile;

/** Powah orb bridge — only loaded when methods are invoked under powah-present guards. */
public final class PowahBridge {
    private PowahBridge() {
    }

    public static BlockPos findNearbyOrb(Level level, BlockPos from, BlockPos remembered) {
        if (level == null || !ModList.get().isLoaded("powah")) {
            return null;
        }
        if (remembered != null && !remembered.equals(BlockPos.ZERO)) {
            BlockEntity be = level.getBlockEntity(remembered);
            if (be instanceof EnergizingOrbTile) {
                return remembered;
            }
        }
        if (from == null || from.equals(BlockPos.ZERO)) {
            return null;
        }
        int range = 4;
        try {
            range = Powah.config().general.energizing_range;
        } catch (Throwable ignored) {
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                from.offset(-range, -range, -range),
                from.offset(range, range, range))) {
            if (level.getBlockEntity(pos) instanceof EnergizingOrbTile) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** @return FE accepted by the orb. */
    public static long feedNearbyOrb(Level level, BlockPos partPos, BlockPos rememberedOrb,
                                     long availableFe, long transfer) {
        if (level == null || availableFe <= 0 || !ModList.get().isLoaded("powah")) {
            return 0;
        }
        BlockPos orbP = findNearbyOrb(level, partPos, rememberedOrb);
        if (orbP == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(orbP);
        if (!(be instanceof EnergizingOrbTile orb) || !orb.containRecipe()) {
            return 0;
        }
        long fill = Math.min(availableFe, transfer);
        if (fill <= 0) {
            return 0;
        }
        return Math.max(0L, orb.fillEnergy(fill));
    }
}
