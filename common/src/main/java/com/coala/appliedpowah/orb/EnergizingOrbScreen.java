package com.coala.appliedpowah.orb;

import appeng.client.gui.Icon;
import appeng.core.AppEng;
import appeng.client.guidebook.PageAnchor;
import com.coala.appliedpowah.network.APNetwork;
import com.coala.appliedpowah.network.C2SOpenPatternMenu;
import com.coala.appliedpowah.network.C2SToggleAutoExport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Energizing orb GUI.
 * Progress / auto-export read from menu DataSlots (server→client every tick)
 * so the progress bar actually animates.
 *
 * Layering:
 * bg shell → progress sprite fill → AE2 toolbar icons.
 */
public class EnergizingOrbScreen extends AbstractContainerScreen<EnergizingOrbMenu> {

    private static final ResourceLocation TEX_ME =
            new ResourceLocation("applied_powah", "textures/gui/fluix_energizing_orb.png");
    private static final ResourceLocation TEX_ADV =
            new ResourceLocation("applied_powah", "textures/gui/advanced_energizing_orb.png");
    private static final ResourceLocation TEX_AUTO =
            new ResourceLocation("applied_powah", "textures/gui/me_energizing_orb.png");
    /** Own icon atlas (asset/orb/results/states.png). */
    private static final ResourceLocation TEX_STATES =
            new ResourceLocation("applied_powah", "textures/gui/states.png");
    // UV in TEX_STATES (256×256)
    private static final int ICON_CANCEL_X = 0;
    private static final int ICON_EXPORT_ON_X = 16;
    private static final int ICON_PATTERN_X = 32;
    private static final int ICON_Y = 0;

    private static final int GUI_W = 176;
    private static final int GUI_H = 199;
    private static final int TEX_SIZE = 256;

    // Texture atlas offsets within the 256×256 sheet
    private static final int TEX_PROG_X = 176;

    /** Vertical progress beside output (6×18). */
    private static final int PROG_W = 6;
    private static final int PROG_H = 18;

    private static final ResourceLocation GUIDE_PAGE =
            new ResourceLocation("applied_powah", "ap_intro/energizing-orbs.md");

    /** AE2 upgrade panel chrome (UpgradesPanel). */
    private static final ResourceLocation TEX_EXTRA_PANELS =
            new ResourceLocation("ae2", "textures/guis/extra_panels.png");
    /** AE2 empty-slot icon BACKGROUND_UPGRADE (Icon 240,208). */
    private static final ResourceLocation TEX_STATES_AE2 =
            new ResourceLocation("ae2", "textures/guis/states.png");

    private static final int BTN = 16;
    private static final int TOOLBAR_X = -20;
    private static final int GUIDE_BTN_Y = 8;
    private static final int EXPORT_BTN_Y = 28;
    private static final int PATTERN_BTN_Y = 48;

    /** Upgrade panel layout (mirrors AE2 UpgradesPanel: pad 7, slot 18). */
    private static final int UP_PAD = 7;
    private static final int UP_SLOT = 18;
    private static final int UP_X = 174; // right:-2 of 176-wide GUI
    private static final int UP_Y = 18;
    private boolean hoverUpgradePanel;
    private int hoverUpgradeSlot = -1;

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

    /**
     * AE2 UpgradesPanel-style column: extra_panels.png slot chrome + empty
     * BACKGROUND_UPGRADE icon (states.png 240,208).
     */
    private void drawUpgradePanel(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int slots = UpgradeCardInv.SLOTS;
        int panelW = UP_PAD * 2 + UP_SLOT;
        int panelH = UP_PAD * 2 + slots * UP_SLOT;
        // outer border pieces from extra_panels.png (128×128), slot cell at src(7,7,18,18)
        g.blit(TEX_EXTRA_PANELS, x + UP_X, y + UP_Y, 0, 0, UP_PAD, UP_PAD, 128, 128);
        g.blit(TEX_EXTRA_PANELS, x + UP_X + UP_PAD + UP_SLOT, y + UP_Y, 25, 0, UP_PAD, UP_PAD, 128, 128);
        g.blit(TEX_EXTRA_PANELS, x + UP_X, y + UP_Y + UP_PAD + slots * UP_SLOT, 0, 25, UP_PAD, UP_PAD, 128, 128);
        g.blit(TEX_EXTRA_PANELS, x + UP_X + UP_PAD + UP_SLOT, y + UP_Y + UP_PAD + slots * UP_SLOT,
                25, 25, UP_PAD, UP_PAD, 128, 128);
        // side rails
        for (int i = 0; i < slots; i++) {
            g.blit(TEX_EXTRA_PANELS, x + UP_X + UP_PAD, y + UP_Y + UP_PAD + i * UP_SLOT, 7, 7, UP_SLOT, UP_SLOT, 128, 128);
            // empty upgrade icon
            var slot = menu.getUpgradeSlot(i);
            if (slot != null && !slot.hasItem()) {
                g.blit(TEX_STATES_AE2,
                        x + UP_X + UP_PAD + 1, y + UP_Y + UP_PAD + i * UP_SLOT + 1,
                        240, 208, 16, 16, 256, 256);
            }
        }
        // hover tracking
        hoverUpgradePanel = false;
        hoverUpgradeSlot = -1;
        int mx = mouseX - x;
        int my = mouseY - y;
        if (mx >= UP_X && mx < UP_X + panelW && my >= UP_Y && my < UP_Y + panelH) {
            hoverUpgradePanel = true;
            int row = (my - UP_Y - UP_PAD) / UP_SLOT;
            if (row >= 0 && row < slots) {
                hoverUpgradeSlot = row;
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (menu.isAdvanced() && hoverUpgradePanel) {
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            lines.add(net.minecraft.network.chat.Component.translatable("applied_powah.gui.compatible_upgrades"));
            lines.add(net.minecraft.network.chat.Component.translatable(
                    "applied_powah.gui.compatible_upgrade.thread"));
            lines.add(net.minecraft.network.chat.Component.translatable(
                    "applied_powah.gui.compatible_upgrade.parallel"));
            lines.add(net.minecraft.network.chat.Component.translatable(
                    "applied_powah.gui.compatible_upgrade.energy"));
            if (hoverUpgradeSlot >= 0) {
                var slot = menu.getUpgradeSlot(hoverUpgradeSlot);
                if (slot != null && slot.hasItem()) {
                    g.renderTooltip(font, slot.getItem(), mx, my);
                    return;
                }
            }
            g.renderComponentTooltip(font, lines, mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }

    /** Lane / T / P status line (GTO-style summary). */
    private void renderLaneStatus(GuiGraphics g) {
        if (!(menu.getOrb() instanceof AdvancedEnergizingOrbBlockEntity adv)) {
            return;
        }
        var status = adv.laneStatusSummary();
        if (status.isEmpty()) {
            return;
        }
        g.drawString(font, status, 8, topPos + 90, 0xFF808080, false);
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

    private int patternX() {
        return leftPos + TOOLBAR_X;
    }

    private int patternY() {
        return topPos + PATTERN_BTN_Y;
    }

    private boolean inBtn(int mx, int my, int x, int y) {
        return mx >= x && mx < x + BTN && my >= y && my < y + BTN;
    }

    // Layout helpers — with-4-block (Advanced/ME) vs without-4-block (Fluix)
    // Exact coords from 最新with-4-block.json / 最新without-4-block.json「进度条（GUI内）」
    private int progX() {
        return menu.isAdvanced() ? 135 : 146;
    }

    private int progY() {
        return 47;
    }

    private boolean hoverProgress(int mx, int my) {
        return mx >= leftPos + progX() - 2 && mx < leftPos + progX() + PROG_W + 2
                && my >= topPos + progY() && my < topPos + progY() + PROG_H;
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
        // Upgrade-card panel outside main GUI (AE2 extra_panels.png chrome)
        if (menu.isAdvanced()) {
            drawUpgradePanel(g, x, topPos, mouseX, mouseY);
        }
        int y = topPos;
        boolean adv = menu.isAdvanced();
        ResourceLocation tex = menu.isAuto() ? TEX_AUTO : (adv ? TEX_ADV : TEX_ME);
        int px = progX();
        int py = progY();

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

        // 3) left toolbar — AE2 states.png
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
        int exportU = on ? ICON_EXPORT_ON_X : ICON_CANCEL_X;
        g.blit(TEX_STATES, exportX(), exportY(), exportU, ICON_Y, 16, 16, 256, 256);
        if (menu.isAuto()) {
            g.fill(patternX() - 1, patternY() - 1, patternX() + BTN + 1, patternY() + BTN + 1, 0xFF373737);
            g.fill(patternX(), patternY(), patternX() + BTN, patternY() + BTN, 0xFF8B8B8B);
            g.blit(TEX_STATES, patternX(), patternY(), ICON_PATTERN_X, ICON_Y, 16, 16, 256, 256);
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
            if (menu.isAuto() && inBtn(mx, my, patternX(), patternY())) {
                var orb = menu.getOrb();
                if (orb != null) {
                    APNetwork.sendToServer(new C2SOpenPatternMenu(orb.getBlockPos()));
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
        renderLaneStatus(g);

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
                    leftPos + 8, topPos + 87, 0xFF404040, false);
        }

        if (inBtn(mouseX, mouseY, guideX(), guideY())) {
            g.renderTooltip(font, Component.translatable("applied_powah.gui.guide"), mouseX, mouseY);
        } else if (inBtn(mouseX, mouseY, exportX(), exportY())) {
            g.renderTooltip(font, Component.translatable(
                    menu.getGuiAutoExport()
                            ? "applied_powah.gui.auto_export.on"
                            : "applied_powah.gui.auto_export.off"),
                    mouseX, mouseY);
        } else if (menu.isAuto() && inBtn(mouseX, mouseY, patternX(), patternY())) {
            g.renderTooltip(font, Component.translatable("applied_powah.gui.patterns"), mouseX, mouseY);
        } else if (max > 0 && hoverProgress(mouseX, mouseY)) {
            int pct = max <= 0 ? 0 : (int) Math.round(100.0 * prog / max);
            g.renderTooltip(font, Component.translatable(
                    "applied_powah.tooltip.progress", fmt(prog), fmt(max), pct), mouseX, mouseY);
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }
}
