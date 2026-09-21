package com.coala.appliedpowah.integration.powah;

import com.coala.appliedpowah.orb.EnergyAcceptingOrb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import owmii.powah.Powah;
import owmii.powah.block.energizing.EnergizingOrbTile;

/**
 * Finds and feeds Powah orbs and Applied Powah orbs (ME / Advanced).
 */
public final class PowahBridge {
    private PowahBridge() {
    }

    private static boolean isFeedable(BlockEntity be) {
        return be instanceof EnergizingOrbTile || be instanceof EnergyAcceptingOrb;
    }

    private static boolean hasRecipe(BlockEntity be) {
        if (be instanceof EnergizingOrbTile powah) {
            return powah.containRecipe();
        }
        if (be instanceof EnergyAcceptingOrb ap) {
            return ap.containRecipe();
        }
        return false;
    }

    private static long fill(BlockEntity be, long amount) {
        if (be instanceof EnergizingOrbTile powah) {
            return Math.max(0L, powah.fillEnergy(amount));
        }
        if (be instanceof EnergyAcceptingOrb ap) {
            return Math.max(0L, ap.fillEnergy(amount));
        }
        return 0;
    }

    public static BlockPos findNearbyOrb(Level level, BlockPos from, BlockPos remembered) {
        if (level == null) {
            return null;
        }
        if (remembered != null && !remembered.equals(BlockPos.ZERO)) {
            BlockEntity be = level.getBlockEntity(remembered);
            if (isFeedable(be)) {
                return remembered;
            }
        }
        if (from == null || from.equals(BlockPos.ZERO)) {
            return null;
        }
        int range = 4;
        if (ModList.get().isLoaded("powah")) {
            try {
                range = Powah.config().general.energizing_range;
            } catch (Throwable ignored) {
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                from.offset(-range, -range, -range),
                from.offset(range, range, range))) {
            if (isFeedable(level.getBlockEntity(pos))) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** @return FE accepted by the orb. */
    public static long feedNearbyOrb(Level level, BlockPos partPos, BlockPos rememberedOrb,
                                     long availableFe, long transfer) {
        if (level == null || availableFe <= 0) {
            return 0;
        }
        BlockPos orbP = findNearbyOrb(level, partPos, rememberedOrb);
        if (orbP == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(orbP);
        if (!hasRecipe(be)) {
            return 0;
        }
        long amount = Math.min(availableFe, transfer);
        if (amount <= 0) {
            return 0;
        }
        return fill(be, amount);
    }
}
