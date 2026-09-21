package com.coala.appliedpowah.guide;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

/**
 * Guide pages live in {@code assets/<ns>/ae2guide/} so AE2's GuideME book
 * merges them into the same navigation tree as AE2 and other addons
 * (ExtendedAE, Advanced AE, …) — see AE2 {@code guidebook.md}.
 *
 * This helper opens the AE2 guide at the Applied Powah index page.
 * Hold-G uses {@code item_ids} frontmatter on those merged pages.
 */
public final class APGuideMe {
    /** AE2's guide id (AppEngClient.GUIDE_ID). */
    public static final ResourceLocation AE2_GUIDE_ID = new ResourceLocation("ae2", "guide");
    /** Page id inside the merged AE2 guide (namespace = our mod). */
    public static final ResourceLocation AP_INDEX_PAGE =
            new ResourceLocation(AppliedPowah.MOD_ID, "ap_intro/ap_intro-index.md");

    private APGuideMe() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("guideme") && ModList.get().isLoaded("ae2");
    }

    @OnlyIn(Dist.CLIENT)
    public static void open() {
        if (!isAvailable()) {
            AppliedPowah.LOG.warn("GuideME/AE2 missing — cannot open guide");
            return;
        }
        try {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            guideme.GuidesCommon.openGuide(player, AE2_GUIDE_ID,
                    new guideme.PageAnchor(AP_INDEX_PAGE, null));
        } catch (Throwable t) {
            AppliedPowah.LOG.error("Failed to open AE2 guide at Applied Powah page", t);
            try {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    guideme.GuidesCommon.openGuide(player, AE2_GUIDE_ID);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** Pages are data-driven under ae2guide/; no separate Guide.builder registration. */
    @OnlyIn(Dist.CLIENT)
    public static void registerClient() {
        if (!ModList.get().isLoaded("guideme")) {
            AppliedPowah.LOG.warn("GuideME missing — ae2guide pages will not show until AE2/GuideME load");
        } else {
            AppliedPowah.LOG.info("Applied Powah guide pages expected under assets/*/ae2guide/ (merged by AE2)");
        }
    }
}
