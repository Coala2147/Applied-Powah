package com.coala.appliedpowah.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.TooltipProvider;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.client.TooltipStyle;
import com.coala.appliedpowah.orb.EnergizingOrbBlock;
import com.coala.appliedpowah.orb.EnergizingOrbBlockEntity;

/**
 * In-game tooltip (Jade / WTHIT / TOP) contribution, registered through AE2's
 * addon abstraction {@link TooltipProvider} (ServiceLoader, see
 * {@code META-INF/services/appeng.api.integrations.igtooltip.TooltipProvider}).
 * <p>
 * Why this and not a hand-written {@code @WailaPlugin}: AE2's own Jade module
 * already wires every addon-registered provider into Jade/WTHIT/TOP with AE2's
 * formatting. We only add what AE2's default providers cannot know — the
 * energizing recipe progress of our orbs. Everything else (grid node state,
 * stored AE for cells / rods / Advanced+ME orbs) comes from AE2's stock
 * {@code PowerStorageDataProvider} / {@code GridNodeStateDataProvider}, because
 * our block entities extend {@code AEBaseBlockEntity} and implement
 * {@code IAEPowerStorage} where they store energy.
 * <p>
 * Class loading safety: this class is only touched when at least one overlay
 * mod (Jade/WTHIT/TOP) is installed, since AE2's {@code TooltipProviders}
 * loader lives in those integration modules. It references AE2 API and our own
 * classes only — never an overlay-mod class — so it is inert otherwise.
 */
public final class APTooltipProvider implements TooltipProvider {

    private static final String TAG_RECIPE_ENERGY = "recipeEnergy";
    private static final String TAG_RECIPE_PROGRESS = "recipeProgress";

    private static final ResourceLocation ORB_PROGRESS_ID =
            new ResourceLocation(AppliedPowah.MOD_ID, "orb_progress");

    @Override
    public void registerCommon(CommonRegistration registration) {
        registration.addBlockEntityData(EnergizingOrbBlockEntity.class, (player, orb, serverData) -> {
            long need = orb.getRecipeEnergy();
            if (need > 0) {
                serverData.putLong(TAG_RECIPE_ENERGY, need);
                serverData.putLong(TAG_RECIPE_PROGRESS, Math.min(need, orb.getProgress()));
            }
        });
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        registration.addBlockEntityBody(
                EnergizingOrbBlockEntity.class,
                EnergizingOrbBlock.class,
                ORB_PROGRESS_ID,
                APTooltipProvider::buildOrbProgress);
    }

    private static void buildOrbProgress(EnergizingOrbBlockEntity orb, TooltipContext context,
            TooltipBuilder tooltip) {
        CompoundTag tag = context.serverData();
        if (!tag.contains(TAG_RECIPE_ENERGY, Tag.TAG_LONG)) {
            return; // no recipe loaded — overlay stays as clean as AE2's
        }
        long need = tag.getLong(TAG_RECIPE_ENERGY);
        long done = Math.min(need, tag.getLong(TAG_RECIPE_PROGRESS));
        int pct = need <= 0 ? 0 : (int) Math.round(100.0 * done / need);
        tooltip.addLine(Component
                .translatable("applied_powah.tooltip.progress",
                        TooltipStyle.formatAmount(done),
                        TooltipStyle.formatAmount(need),
                        pct)
                .withStyle(ChatFormatting.GRAY));
    }
}
