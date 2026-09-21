package com.coala.appliedpowah.guide;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;

/**
 * GuideME book for Applied Powah.
 *
 * Official integration (https://guideme.appliedenergistics.org/integration/):
 * {@code Guide.builder(id).build()} in mod construction; pages live under the
 * guide folder (default {@code guides/<ns>/<path>}; we override with
 * {@code applied_powah_guide}, same pattern as AE2's {@code ae2guide}).
 *
 * Hold-G on item tooltips is enabled by listing item ids in page frontmatter
 * {@code item_ids} (https://guideme.appliedenergistics.org/ authoring + hotkey docs).
 */
public final class APGuideMe {
    public static final ResourceLocation GUIDE_ID =
            new ResourceLocation(AppliedPowah.MOD_ID, "guide");
    /** Resource folder: assets/applied_powah/applied_powah_guide/ */
    public static final String FOLDER = "applied_powah_guide";
    public static final ResourceLocation START_PAGE =
            new ResourceLocation(AppliedPowah.MOD_ID, "index.md");

    @Nullable
    @OnlyIn(Dist.CLIENT)
    private static Object guideHandle;

    private APGuideMe() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("guideme");
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerClient() {
        if (!isAvailable()) {
            AppliedPowah.LOG.warn("GuideME missing — Applied Powah guide will not open (AE2 normally requires GuideME)");
            return;
        }
        try {
            guideHandle = guideme.Guide.builder(GUIDE_ID)
                    .defaultNamespace(AppliedPowah.MOD_ID)
                    .folder(FOLDER)
                    .startPage(START_PAGE)
                    .build();
            AppliedPowah.LOG.info("GuideME book registered id={} folder={} start={}",
                    GUIDE_ID, FOLDER, START_PAGE);
        } catch (Throwable t) {
            AppliedPowah.LOG.error("GuideME registration failed", t);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void open() {
        if (!isAvailable()) {
            return;
        }
        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                guideme.GuidesCommon.openGuide(player, GUIDE_ID);
            }
        } catch (Throwable t) {
            AppliedPowah.LOG.error("Failed to open Applied Powah guide", t);
        }
    }
}
