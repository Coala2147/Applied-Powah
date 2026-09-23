package com.coala.appliedpowah.orb;

/**
 * Pure nearest-candidate selection (rod → orb). No Minecraft types — unit-testable.
 */
public final class NearestPos {

    private NearestPos() {
    }

    /**
     * Index of the nearest candidate to (fx,fy,fz); ties prefer {@code preferredIndex}.
     *
     * @return candidate index, or -1 if none
     */
    public static int nearestIndex(long[] xs, long[] ys, long[] zs, long fx, long fy, long fz, int preferredIndex) {
        if (xs == null || ys == null || zs == null) {
            return -1;
        }
        int n = Math.min(xs.length, Math.min(ys.length, zs.length));
        int best = -1;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            long d = distSq(xs[i], ys[i], zs[i], fx, fy, fz);
            if (best < 0 || d < bestDist || (d == bestDist && i == preferredIndex)) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    public static long distSq(long x1, long y1, long z1, long x2, long y2, long z2) {
        long dx = x1 - x2;
        long dy = y1 - y2;
        long dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Keep {@code remembered} only when it is still the nearest candidate.
     *
     * @return nearest index (possibly {@code rememberedIndex}), or -1
     */
    public static int selectStable(long[] xs, long[] ys, long[] zs, long fx, long fy, long fz, int rememberedIndex) {
        return nearestIndex(xs, ys, zs, fx, fy, fz, rememberedIndex);
    }
}
