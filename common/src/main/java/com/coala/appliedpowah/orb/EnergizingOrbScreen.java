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

    private static final int GUI_W = 176;
    private static final int GUI_H = 199;
    private static final int TEX_SIZE = 256;

    // Texture atlas offsets within the 256×256 sheet
    private static final int TEX_PROG_X = 176;
    private static final int TEX_ALERT_X = 182;

    /** Vertical progress beside output (6×18). */
    private static final int PROG_W = 6;
    private static final int PROG_H = 18;

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
        // Slot row 0 begins at y=114; keep 12 px gap like vanilla 166-height screens
        this.inventoryLabelY = this.imageHeight - 97;
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

    // Layout helpers — ME vs Advanced differ per latest JSON specs (+1,+1 shift)
    private int progX() {
        return menu.isAdvanced() ? 136 : 147;
    }

    private int progY() {
        return 48;
    }

    private int alertX() {
        return menu.isAdvanced() ? 113 : 124;
    }

    private int alertY() {
        return 48;
    }

    private boolean hoverProgress(int mx, int my) {
        return mx >= leftPos + progX() - 2 && mx < leftPos + progX() + PROG_W + 2
                && my >= topPos + progY() && my < topPos + progY() + PROG_H;
    }

    private boolean hoverAlert(int mx, int my) {
        return mx >= leftPos + alertX() && mx < leftPos + alertX() + ALERT_SIZE
                && my >= topPos + alertY() && my < topPos + alertY() + ALERT_SIZE;
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
        ResourceLocation tex = adv ? TEX_ADV : TEX_ME;
        int px = progX();
        int py = progY();
        int ax = alertX();
        int ay = alertY();

        // 1) empty-shell background (176×199 from the 256×256 sheet)
        g.blit(tex, x, y, 0, 0, GUI_W, GUI_H, TEX_SIZE, TEX_SIZE);

        // 2) progress fill from DataSlot values — bar moves as energy fills
        long prog = menu.getGuiProgress();
        long max = menu.getGuiRecipeEnergy();
        if (max > 0) {
            int fillH = (int) ((prog * (long) PROG_H) / max);
            if (fillH > PROG_H) {
                fillH = PROG_H;
            }
            if (fillH > 0) {
                // fill from bottom (VERTICAL): bottom-up bar
                int top = y + py + (PROG_H - fillH);
                int srcY = PROG_H - fillH;
                try {
                    g.blit(tex, x + px, top, TEX_PROG_X, srcY, PROG_W, fillH, TEX_SIZE, TEX_SIZE);
                } catch (Throwable t) {
                    g.fill(x + px, top, x + px + PROG_W, y + py + PROG_H, 0xFF00C853);
                }
            }
        }

        // 3) power alert overlaid on output slot (DataSlot)
        if (menu.getGuiShowWarning()) {
            try {
                g.blit(tex, x + ax, y + ay, TEX_ALERT_X, 0, ALERT_SIZE, ALERT_SIZE,
                        TEX_SIZE, TEX_SIZE);
            } catch (Throwable t) {
                g.fill(x + ax + 2, y + ay + 2,
                        x + ax + ALERT_SIZE - 2, y + ay + ALERT_SIZE - 2, 0xFFE65100);
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
            // Fixed positions from user YAML (with-4-block:57,45 / without-4-block:68,45)
            int energyX = menu.isAdvanced() ? leftPos + 57 : leftPos + 68;
            g.drawString(font, fmt(prog) + "/" + fmt(max),
                    energyX, topPos + 45, 0xFF404040, false);
        }
        if (menu.isAdvanced() && menu.getGuiCapacity() > 0) {
            g.drawString(font,
                    "缓存 " + fmt(menu.getGuiEnergy()) + "/" + fmt(menu.getGuiCapacity())
                            + " " + menu.getGuiEnergyUnit(),
                    leftPos + 13, topPos + 93, 0xFF404040, false);
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
