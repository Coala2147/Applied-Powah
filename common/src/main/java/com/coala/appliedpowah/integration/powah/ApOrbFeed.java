package com.coala.appliedpowah.integration.powah;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.orb.EnergyAcceptingOrb;
import com.coala.appliedpowah.orb.NearestPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * AP-only orb feed (no owmii.* imports). Used when Powah is absent so rods can
 * still target {@link EnergyAcceptingOrb} implementations.
 */
public final class ApOrbFeed {
    private ApOrbFeed() {
    }

    public static BlockPos findNearestApOrb(Level level, BlockPos from, BlockPos remembered, int range) {
        if (level == null || from == null || from.equals(BlockPos.ZERO)) {
            return null;
        }
        List<BlockPos> candidates = new ArrayList<>();
        int rememberedIndex = -1;
        for (BlockPos pos : BlockPos.betweenClosed(
                from.offset(-range, -range, -range),
                from.offset(range, range, range))) {
            // Only rod-feedable tiers (Fluix); Advanced/ME excluded (issues.md 0.6.0-2).
            if (!(level.getBlockEntity(pos) instanceof EnergyAcceptingOrb ap) || !ap.rodFeedable()) {
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

    public static long feedNearestApOrb(Level level, BlockPos partPos, BlockPos rememberedOrb,
                                        long availableFe, long transfer, int range) {
        if (level == null || availableFe <= 0) {
            return 0;
        }
        BlockPos orbP = findNearestApOrb(level, partPos, rememberedOrb, range);
        if (orbP == null) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(orbP);
        if (!(be instanceof EnergyAcceptingOrb ap) || !ap.containRecipe()) {
            return 0;
        }
        long amount = Math.min(availableFe, transfer);
        if (amount <= 0) {
            return 0;
        }
        try {
            return Math.max(0L, ap.fillEnergy(amount));
        } catch (Exception e) {
            AppliedPowah.LOG.warn("ApOrbFeed.fillEnergy failed at {}: {}", orbP, e.toString());
            return 0;
        }
    }
}
