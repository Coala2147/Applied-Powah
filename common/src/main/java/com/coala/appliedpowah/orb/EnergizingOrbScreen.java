package com.coala.appliedpowah.orb;

import appeng.client.gui.Icon;
import appeng.client.guidebook.PageAnchor;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ProgressBar;
import com.coala.appliedpowah.meorb.OrbDirectionsScreen;
import com.coala.appliedpowah.network.APNetwork;
import com.coala.appliedpowah.network.C2SOpenPatternMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Energizing orb GUI — AE2 style screen with:
 * <ul>
 *   <li>per-tier ScreenStyle JSON (background, slot positions, progress bar);</li>
 *   <li>left toolbar: page arrows, eject tri-state button, output-face config
 *       (SIDES mode only), ME pattern page;</li>
 *   <li>page label, reaction energy, cache line and power alert in the foreground.</li>
 * </ul>
 */
public class EnergizingOrbScreen extends UpgradeableScreen<EnergizingOrbMenu> {

    private static final ResourceLocation TEX_STATES =
            new ResourceLocation("applied_powah", "textures/gui/states.png");
    /** states.png sprite of the pattern-page button. */
    private static final int ICON_PATTERN_X = 32;
    private static final int ICON_PATTERN_Y = 0;

    private static final ResourceLocation GUIDE_PAGE =
            new ResourceLocation("applied_powah", "ap_intro/energizing-orbs.md");

    /** Text positions inside the 176×199 GUI (free panel areas of the art set). */
    private static final int PAGE_LABEL_X = 8;
    private static final int PAGE_LABEL_Y = 18;
    private static final int ENERGY_X_ADV = 57;
    private static final int ENERGY_X_FLUIX = 68;
    private static final int ENERGY_Y = 45;
    private static final int CACHE_X = 8;
    private static final int CACHE_Y = 87;

    private final ProgressBar progressBar;
    private final IconButton prevPage;
    private final IconButton nextPage;
    private final IconButton ejectButton;
    private final IconButton directionsButton;
    private final PatternPageButton patternButton;
    private final AlertWidget powerAlert;

    public EnergizingOrbScreen(EnergizingOrbMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.progressBar = new ProgressBar(menu, style.getImage("progressBar"),
                ProgressBar.Direction.VERTICAL);
        widgets.add("progressBar", this.progressBar);

        this.prevPage = new IconButton(btn -> menu.requestPage(menu.getPage() - 1)) {
            @Override
            protected Icon getIcon() {
                return Icon.ARROW_LEFT;
            }
        };
        this.prevPage.setMessage(Component.translatable("applied_powah.gui.page.prev"));
        addToLeftToolbar(this.prevPage);

        this.nextPage = new IconButton(btn -> menu.requestPage(menu.getPage() + 1)) {
            @Override
            protected Icon getIcon() {
                return Icon.ARROW_RIGHT;
            }
        };
        this.nextPage.setMessage(Component.translatable("applied_powah.gui.page.next"));
        addToLeftToolbar(this.nextPage);

        this.ejectButton = new IconButton(btn -> menu.cycleEject()) {
            @Override
            protected Icon getIcon() {
                return switch (menu.getEjectMode()) {
                    case OFF -> Icon.AUTO_EXPORT_OFF;
                    case SIDES -> Icon.INSCRIBER_SEPARATE_SIDES;
                    case NETWORK -> Icon.AUTO_EXPORT_ON;
                };
            }
        };
        this.ejectButton.setMessage(ejectTooltip());
        addToLeftToolbar(this.ejectButton);

        this.directionsButton = new IconButton(
                btn -> switchToScreen(new OrbDirectionsScreen(this))) {
            @Override
            protected Icon getIcon() {
                return Icon.WRENCH;
            }
        };
        this.directionsButton.setMessage(Component.translatable("applied_powah.gui.eject.sides_config"));
        addToLeftToolbar(this.directionsButton);

        this.patternButton = new PatternPageButton(btn -> {
            var orb = menu.getOrb();
            if (orb != null) {
                APNetwork.sendToServer(new C2SOpenPatternMenu(orb.getBlockPos()));
            }
        });
        widgets.add("patternPage", this.patternButton);

        this.powerAlert = new AlertWidget(style.getImage("powerAlert"));
        widgets.add("powerAlert", this.powerAlert);
    }

    private Component ejectTooltip() {
        return Component.translatable(menu.getEjectMode().langKey());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.menu.showPage();

        boolean multi = this.menu.pageCount() > 1;
        this.prevPage.setVisibility(multi && this.menu.getPage() > 0);
        this.nextPage.setVisibility(multi && this.menu.getPage() < this.menu.pageCount() - 1);
        this.ejectButton.setMessage(ejectTooltip());
        this.directionsButton.setVisibility(this.menu.showsDirectionsButton());
        this.patternButton.visible = this.menu.isAuto();
        this.powerAlert.visible = this.menu.isShowWarning();

        long prog = this.menu.getGuiProgress();
        long max = this.menu.getGuiRecipeEnergy();
        if (max > 0) {
            int pct = (int) Math.min(100, prog * 100 / max);
            this.progressBar.setFullMsg(Component.translatable("applied_powah.tooltip.progress",
                    String.valueOf(prog), String.valueOf(max), String.valueOf(pct)));
        } else {
            this.progressBar.setFullMsg(Component.empty());
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        int textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        if (this.menu.pageCount() > 1) {
            guiGraphics.drawString(font,
                    Component.translatable("applied_powah.gui.page.label",
                            this.menu.getPage() + 1, this.menu.pageCount()),
                    PAGE_LABEL_X, PAGE_LABEL_Y, textColor, false);
        }

        long prog = this.menu.getGuiProgress();
        long max = this.menu.getGuiRecipeEnergy();
        if (max > 0) {
            int x = this.menu.isAdvanced() ? ENERGY_X_ADV : ENERGY_X_FLUIX;
            guiGraphics.drawString(font, fmt(prog) + "/" + fmt(max), x, ENERGY_Y, textColor, false);
        }

        if (this.menu.isAdvanced() && this.menu.getGuiCapacity() > 0) {
            guiGraphics.drawString(font,
                    Component.translatable("applied_powah.gui.energy_cache").getString() + " "
                            + fmt(this.menu.getGuiEnergy()) + "/" + fmt(this.menu.getGuiCapacity())
                            + " " + this.menu.getGuiEnergyUnit(),
                    CACHE_X, CACHE_Y, textColor, false);
        }
    }

    @Override
    protected appeng.client.guidebook.PageAnchor getHelpTopic() {
        return PageAnchor.page(GUIDE_PAGE);
    }

    private static String fmt(long v) {
        if (v >= 1_000_000L) {
            return (v / 1_000_000L) + "M";
        }
        if (v >= 1_000L) {
            return (v / 1_000L) + "k";
        }
        return String.valueOf(v);
    }

    /** Reaction-chamber style power alert (18×18 sprite from the ScreenStyle). */
    private static final class AlertWidget extends AbstractWidget {

        private final Blitter icon;

        AlertWidget(Blitter icon) {
            super(0, 0, 18, 18, Component.empty());
            this.icon = icon;
            this.visible = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (icon != null && this.visible) {
                icon.dest(getX(), getY()).blit(guiGraphics);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    /** Pattern-page button (ME tier only): sprite from the mod's own icon atlas. */
    private static final class PatternPageButton extends Button {

        PatternPageButton(OnPress onPress) {
            super(0, 0, 16, 16, Component.translatable("applied_powah.gui.patterns"), onPress,
                    DEFAULT_NARRATION);
            this.visible = false;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (this.visible) {
                guiGraphics.blit(TEX_STATES, getX(), getY(), ICON_PATTERN_X, ICON_PATTERN_Y,
                        16, 16, 256, 256);
            }
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput narration) {
            narration.add(NarratedElementType.TITLE, this.getMessage());
        }
    }
}
