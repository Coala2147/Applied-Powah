package com.coala.appliedpowah.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * AE2/Powah-aligned tooltip composition.
 * Labels: light gray ({@link ChatFormatting#GRAY}).
 * Numeric payload (e.g. {@code 0/20M FE}): deeper {@link ChatFormatting#DARK_GRAY},
 * matching Powah energy tooltips and AE2 number emphasis.
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

    /** label(GRAY) + ": " + numbers DARK_GRAY — AE2 sentence form. */
    public static MutableComponent labeled(String labelKey, String valueText) {
        return Component.empty()
                .append(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(valueText).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Stored Energy: {cur}/{max} AE ({pct}) */
    public static MutableComponent storedEnergy(double cur, double max) {
        String value = formatAmount(cur) + "/" + formatAmount(max) + " AE ("
                + formatPercent(cur, max) + ")";
        return labeled("applied_powah.tooltip.stored_energy_label", value);
    }

    private static String trim(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
