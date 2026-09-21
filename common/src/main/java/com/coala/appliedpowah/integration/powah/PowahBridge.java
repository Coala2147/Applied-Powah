package com.coala.appliedpowah.integration.powah;

import com.coala.appliedpowah.orb.EnergyAcceptingOrb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import owmii.powah.Powah;
import owmii.powah.block.energizing.EnergizingOrbTile;

/**
 * Powah / AP orb bridge. Rods feed both Powah {@link EnergizingOrbTile}
 * and AP {@link EnergyAcceptingOrb} (ME orb + Advanced orb).
 */
public final class PowahBridge {
    private PowahBridge() {
    }

    public static boolean isFeedableOrb(BlockEntity be) {
        return be instanceof EnergizingOrbTile || be instanceof EnergyAcceptingOrb;
    }

    public static BlockPos findNearbyOrb(Level level, BlockPos from, BlockPos remembered) {
        if (level == null) {
            return null;
        }
        if (remembered != null && !remembered.equals(BlockPos.ZERO)) {
            BlockEntity be = level.getBlockEntity(remembered);
            if (isFeedableOrb(be)) {
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
            if (isFeedableOrb(level.getBlockEntity(pos))) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** @return FE accepted by the orb. */
    public static long feedNearbyOrb(Level level, BlockPos partPos, BlockPos rememberedOrb,
                                     long availableFe, long transfer) {
        if (level == null || availableFe <= 0 || transfer <= 0) {
            return 0;
        }
        BlockPos orbP = findNearbyOrb(level, partPos, rememberedOrb);
        if (orbP == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(orbP);
        long fill = Math.min(availableFe, transfer);
        if (fill <= 0) {
            return 0;
        }
        if (be instanceof EnergizingOrbTile orb) {
            if (!orb.containRecipe()) {
                return 0;
            }
            return Math.max(0L, orb.fillEnergy(fill));
        }
        if (be instanceof EnergyAcceptingOrb orb) {
            if (!orb.containRecipe()) {
                return 0;
            }
            return Math.max(0L, orb.fillEnergy(fill));
        }
        return 0;
    }
}
