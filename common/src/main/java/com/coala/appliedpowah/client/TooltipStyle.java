package com.coala.appliedpowah.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * AE2-aligned tooltip builders.
 * Energy/spec lines use light gray ({@link ChatFormatting#GRAY}), non-italic —
 * matching AE2 {@code NORMAL_TOOLTIP_TEXT} / Powah label styling, not dark gray.
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

    /** AE2 sentence: 已存储能源: {cur}/{max} AE ({pct}) */
    public static MutableComponent storedEnergy(double cur, double max) {
        return Component.translatable("applied_powah.tooltip.stored_energy",
                        formatAmount(cur), formatAmount(max), formatPercent(cur, max))
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent guideHint() {
        return Component.translatable("applied_powah.tooltip.guide_hint")
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent spec(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static String trim(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
