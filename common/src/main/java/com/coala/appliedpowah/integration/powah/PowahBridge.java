package com.coala.appliedpowah.integration.powah;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.orb.EnergyAcceptingOrb;
import com.coala.appliedpowah.orb.NearestPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import owmii.powah.Powah;
import owmii.powah.block.energizing.EnergizingOrbTile;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds and feeds Powah orbs and Applied Powah orbs (ME / Advanced / Fluix).
 * Selection is always the nearest feedable orb (not scan-order first).
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

    static int defaultRange() {
        int range = 4;
        if (ModList.get().isLoaded("powah")) {
            try {
                range = Powah.config().general.energizing_range;
            } catch (Exception e) {
                AppliedPowah.LOG.debug("powah energizing_range unavailable, default 4: {}", e.toString());
            }
        }
        return range;
    }

    /**
     * Nearest feedable orb within range. {@code remembered} wins only when still nearest
     * (stable on ties). Scan-order first-hit is intentionally not used (that was the bug).
     */
    public static BlockPos findNearbyOrb(Level level, BlockPos from, BlockPos remembered) {
        if (level == null) {
            return null;
        }
        if (from == null || from.equals(BlockPos.ZERO)) {
            return null;
        }
        int range = defaultRange();
        List<BlockPos> candidates = new ArrayList<>();
        int rememberedIndex = -1;
        for (BlockPos pos : BlockPos.betweenClosed(
                from.offset(-range, -range, -range),
                from.offset(range, range, range))) {
            if (!isFeedable(level.getBlockEntity(pos))) {
                continue;
            }
            BlockPos imm = pos.immutable();
            if (remembered != null && imm.equals(remembered)) {
                rememberedIndex = candidates.size();
            }
            candidates.add(imm);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        long[] xs = new long[candidates.size()];
        long[] ys = new long[candidates.size()];
        long[] zs = new long[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            BlockPos p = candidates.get(i);
            xs[i] = p.getX();
            ys[i] = p.getY();
            zs[i] = p.getZ();
        }
        int best = NearestPos.nearestIndex(xs, ys, zs, from.getX(), from.getY(), from.getZ(), rememberedIndex);
        return best < 0 ? null : candidates.get(best);
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
