package com.coala.appliedpowah.client;

import java.util.Locale;

/**
 * AE2/Powah-aligned number formatting for tooltips and overlays:
 * compact amounts ({@code 1.2M}) and percentages, matching AE2's
 * {@code Platform.formatPower} conventions.
 */
public final class TooltipStyle {

    private TooltipStyle() {
    }

    public static String formatAmount(double v) {
        if (v >= 1_000_000_000D) {
            return trim(v / 1_000_000_000D) + "G";
        }
        if (v >= 1_000_000D) {
            return trim(v / 1_000_000D) + "M";
        }
        if (v >= 1_000D) {
            return trim(v / 1_000D) + "k";
        }
        return trim(v);
    }

    public static String formatPercent(double cur, double max) {
        if (max <= 0) {
            return "0%";
        }
        double pct = Math.max(0, Math.min(100, (cur / max) * 100.0));
        if (Math.abs(pct - Math.rint(pct)) < 0.05) {
            return (long) Math.rint(pct) + "%";
        }
        return String.format(Locale.ROOT, "%.1f%%", pct);
    }

    private static String trim(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
