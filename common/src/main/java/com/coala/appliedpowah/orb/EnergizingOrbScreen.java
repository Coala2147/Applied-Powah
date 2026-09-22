package com.coala.appliedpowah.orb;

import appeng.client.gui.Icon;
import appeng.core.AppEng;
import appeng.client.guidebook.PageAnchor;
import com.coala.appliedpowah.network.APNetwork;
import com.coala.appliedpowah.network.C2SToggleAutoExport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Energizing orb GUI.
 * Progress / warning / auto-export read from menu DataSlots (server→client every tick)
 * so the progress bar actually animates.
 *
 * Layering (reaction_chamber):
 * bg shell → progress sprite fill → power alert → AE2 toolbar icons.
 */
public class EnergizingOrbScreen extends AbstractContainerScreen<EnergizingOrbMenu> {

    private static final ResourceLocation TEX_ME =
            new ResourceLocation("applied_powah", "textures/gui/me_energizing_orb.png");
    private static final ResourceLocation TEX_ADV =
            new ResourceLocation("applied_powah", "textures/gui/advanced_energizing_orb.png");
    private static final ResourceLocation TEX_PROG =
            new ResourceLocation("applied_powah", "textures/gui/progress_bar.png");
    private static final ResourceLocation TEX_ALERT =
            new ResourceLocation("applied_powah", "textures/gui/power_alert.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    /** Vertical progress beside output (6×18). */
    private static final int PROG_X = 132;
    private static final int PROG_Y = 36;
    private static final int PROG_W = 6;
    private static final int PROG_H = 18;

    private static final int ALERT_X = 117;
    private static final int ALERT_Y = 68;
    private static final int ALERT_SIZE = 18;

    private static final ResourceLocation GUIDE_PAGE =
            new ResourceLocation("applied_powah", "ap_intro/energizing-orbs.md");

    private static final int BTN = 16;
    private static final int TOOLBAR_X = -20;
    private static final int GUIDE_BTN_Y = 8;
    private static final int EXPORT_BTN_Y = 28;

    public EnergizingOrbScreen(EnergizingOrbMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = this.imageHeight - 94;
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

    private int guideX() {
        return leftPos + TOOLBAR_X;
    }

    private int guideY() {
        return topPos + GUIDE_BTN_Y;
    }

    private int exportX() {
        return leftPos + TOOLBAR_X;
    }

    private int exportY() {
        return topPos + EXPORT_BTN_Y;
    }

    private boolean inBtn(int mx, int my, int x, int y) {
        return mx >= x && mx < x + BTN && my >= y && my < y + BTN;
    }

    private boolean hoverProgress(int mx, int my) {
        return mx >= leftPos + PROG_X - 2 && mx < leftPos + PROG_X + PROG_W + 2
                && my >= topPos + PROG_Y && my < topPos + PROG_Y + PROG_H;
    }

    private boolean hoverAlert(int mx, int my) {
        return mx >= leftPos + ALERT_X && mx < leftPos + ALERT_X + ALERT_SIZE
                && my >= topPos + ALERT_Y && my < topPos + ALERT_Y + ALERT_SIZE;
    }

    private void openGuide() {
        try {
            AppEng.instance().openGuideAtAnchor(new PageAnchor(GUIDE_PAGE, null));
        } catch (Throwable t) {
            try {
                AppEng.instance().openGuideAtPreviousPage(GUIDE_PAGE);
            } catch (Throwable ignored) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("applied_powah.gui.guide_missing"), true);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        boolean adv = menu.isAdvanced();

        // 1) empty-shell background
        g.blit(adv ? TEX_ADV : TEX_ME, x, y, 0, 0, GUI_W, GUI_H);

        // 2) progress track (empty) + fill from DataSlot values — bar moves as energy fills
        long prog = menu.getGuiProgress();
        long max = menu.getGuiRecipeEnergy();
        // dark track so the fill is always visible against the shell
        g.fill(x + PROG_X, y + PROG_Y, x + PROG_X + PROG_W, y + PROG_Y + PROG_H, 0xFF373737);
        if (max > 0) {
            int fillH = (int) ((prog * (long) PROG_H) / max);
            if (fillH > PROG_H) {
                fillH = PROG_H;
            }
            if (fillH > 0) {
                // fill from bottom (VERTICAL ProgressBar): bottom-up green bar
                int top = y + PROG_Y + (PROG_H - fillH);
                int srcY = PROG_H - fillH;
                try {
                    g.blit(TEX_PROG, x + PROG_X, top, 0, srcY, PROG_W, fillH, PROG_W, PROG_H);
                } catch (Throwable t) {
                    g.fill(x + PROG_X, top, x + PROG_X + PROG_W, y + PROG_Y + PROG_H, 0xFF00C853);
                }
            }
        }

        // 3) power alert (DataSlot)
        if (menu.getGuiShowWarning()) {
            try {
                g.blit(TEX_ALERT, x + ALERT_X, y + ALERT_Y, 0, 0, ALERT_SIZE, ALERT_SIZE,
                        ALERT_SIZE, ALERT_SIZE);
            } catch (Throwable t) {
                g.fill(x + ALERT_X + 2, y + ALERT_Y + 2,
                        x + ALERT_X + ALERT_SIZE - 2, y + ALERT_Y + ALERT_SIZE - 2, 0xFFE65100);
            }
        }

        // 4) left toolbar — AE2 states.png
        renderToolbarButtons(g);
    }

    private void renderToolbarButtons(GuiGraphics g) {
        g.fill(guideX() - 1, guideY() - 1, guideX() + BTN + 1, guideY() + BTN + 1, 0xFF373737);
        g.fill(guideX(), guideY(), guideX() + BTN, guideY() + BTN, 0xFF8B8B8B);
        g.fill(exportX() - 1, exportY() - 1, exportX() + BTN + 1, exportY() + BTN + 1, 0xFF373737);
        g.fill(exportX(), exportY(), exportX() + BTN, exportY() + BTN, 0xFF8B8B8B);
        try {
            Icon.HELP.getBlitter().dest(guideX(), guideY()).blit(g);
        } catch (Throwable t) {
            g.drawString(font, "?", guideX() + 5, guideY() + 4, 0xFFFFFFFF, false);
        }
        boolean on = menu.getGuiAutoExport();
        try {
            (on ? Icon.AUTO_EXPORT_ON : Icon.AUTO_EXPORT_OFF).getBlitter().dest(exportX(), exportY()).blit(g);
        } catch (Throwable t) {
            g.fill(exportX() + 3, exportY() + 3, exportX() + 13, exportY() + 13,
                    on ? 0xFF00C853 : 0xFFC62828);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (inBtn(mx, my, guideX(), guideY())) {
                openGuide();
                return true;
            }
            if (inBtn(mx, my, exportX(), exportY())) {
                var orb = menu.getOrb();
                if (orb != null) {
                    APNetwork.sendToServer(new C2SToggleAutoExport(orb.getBlockPos()));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);

        long prog = menu.getGuiProgress();
        long max = menu.getGuiRecipeEnergy();
        if (max > 0) {
            g.drawString(font, fmt(prog) + "/" + fmt(max),
                    leftPos + PROG_X - 36, topPos + PROG_Y + PROG_H + 2, 0xFF373737, false);
        }
        if (menu.isAdvanced() && menu.getGuiCapacity() > 0) {
            g.drawString(font,
                    "缓存 " + fmt(menu.getGuiEnergy()) + "/" + fmt(menu.getGuiCapacity())
                            + " " + menu.getGuiEnergyUnit(),
                    leftPos + 28, topPos + 70, 0xFF373737, false);
        }

        if (inBtn(mouseX, mouseY, guideX(), guideY())) {
            g.renderTooltip(font, Component.translatable("applied_powah.gui.guide"), mouseX, mouseY);
        } else if (inBtn(mouseX, mouseY, exportX(), exportY())) {
            g.renderTooltip(font, Component.translatable(
                    menu.getGuiAutoExport()
                            ? "applied_powah.gui.auto_export.on"
                            : "applied_powah.gui.auto_export.off"),
                    mouseX, mouseY);
        } else if (menu.getGuiShowWarning() && hoverAlert(mouseX, mouseY)) {
            g.renderTooltip(font,
                    java.util.List.of(
                            Component.translatable("applied_powah.gui.power_warning")
                                    .withStyle(net.minecraft.ChatFormatting.RED).getVisualOrderText(),
                            Component.translatable("applied_powah.gui.power_warning_details")
                                    .withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText()),
                    mouseX, mouseY);
        } else if (max > 0 && hoverProgress(mouseX, mouseY)) {
            int pct = max <= 0 ? 0 : (int) Math.round(100.0 * prog / max);
            g.renderTooltip(font, Component.translatable(
                    "applied_powah.tooltip.progress", fmt(prog), fmt(max), pct), mouseX, mouseY);
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }
}
