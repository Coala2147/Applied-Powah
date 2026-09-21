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
 * Left of the main panel (AE2 toolbar style):
 * <ol>
 *   <li>HELP (?) — open GuideME at the Applied Powah orb page</li>
 *   <li>AUTO_EXPORT on/off — push products into ME (Interface-style)</li>
 * </ol>
 * Icons reuse AE2 {@code textures/guis/states.png} at runtime.
 */
public class EnergizingOrbScreen extends AbstractContainerScreen<EnergizingOrbMenu> {

    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    private static final int PROG_X = 88;
    private static final int PROG_Y = 44;
    private static final int PROG_W = 16;
    private static final int PROG_H = 6;

    /** GuideME page id for both orbs. */
    private static final ResourceLocation GUIDE_PAGE =
            new ResourceLocation("applied_powah", "ap_intro/energizing-orbs.md");

    private static final int BTN = 16;
    /** Toolbar sits just outside the left edge of the GUI panel. */
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
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF8B8B8B);
        // 6 input slots area
        g.fill(x + 28, y + 12, x + 82, y + 66, 0xFF373737);
        g.fill(x + 30, y + 14, x + 80, y + 64, 0xFF8B8B8B);
        // output slot
        g.fill(x + 110, y + 30, x + 130, y + 50, 0xFF373737);
        g.fill(x + 112, y + 32, x + 128, y + 48, 0xFF8B8B8B);
        // progress trough
        g.fill(x + 88, y + 36, x + 104, y + 42, 0xFF373737);

        var orb = menu.getOrb();
        long prog = orb == null ? 0 : orb.getProgress();
        long max = orb == null ? 0 : orb.getRecipeEnergy();
        if (max > 0) {
            int barX = x + PROG_X;
            int barY = y + PROG_Y;
            g.fill(barX, barY, barX + PROG_W, barY + PROG_H, 0xFF373737);
            int fill = (int) Math.min(PROG_W, (prog * PROG_W) / max);
            g.fill(barX, barY, barX + fill, barY + PROG_H, 0xFF00AA00);
        }

        // Advanced: 4 rod slots column on the right (matches with-4-block mock)
        if (orb instanceof AdvancedEnergizingOrbBlockEntity) {
            g.fill(x + 140, y + 12, x + 168, y + 84, 0xFF373737);
            g.fill(x + 142, y + 14, x + 166, y + 82, 0xFF8B8B8B);
        }
        g.fill(x + 7, y + 83, x + 169, y + 163, 0xFF8B8B8B);

        // Left toolbar (outside panel): help + auto-export, AE2 states.png
        renderToolbarButtons(g);
    }

    private void renderToolbarButtons(GuiGraphics g) {
        // Slot backgrounds
        g.fill(guideX() - 1, guideY() - 1, guideX() + BTN + 1, guideY() + BTN + 1, 0xFF373737);
        g.fill(guideX(), guideY(), guideX() + BTN, guideY() + BTN, 0xFF8B8B8B);
        g.fill(exportX() - 1, exportY() - 1, exportX() + BTN + 1, exportY() + BTN + 1, 0xFF373737);
        g.fill(exportX(), exportY(), exportX() + BTN, exportY() + BTN, 0xFF8B8B8B);

        try {
            Icon.HELP.getBlitter().dest(guideX(), guideY()).blit(g);
        } catch (Throwable t) {
            g.drawString(font, "?", guideX() + 5, guideY() + 4, 0xFFFFFFFF, false);
        }
        var orb = menu.getOrb();
        boolean on = orb != null && orb.isAutoExport();
        try {
            Icon icon = on ? Icon.AUTO_EXPORT_ON : Icon.AUTO_EXPORT_OFF;
            icon.getBlitter().dest(exportX(), exportY()).blit(g);
        } catch (Throwable t) {
            g.fill(exportX() + 3, exportY() + 3, exportX() + 13, exportY() + 13,
                    on ? 0xFF00C853 : 0xFFC62828);
        }
    }

    private boolean hoverProgress(int mx, int my) {
        return mx >= leftPos + PROG_X && mx < leftPos + PROG_X + PROG_W
                && my >= topPos + PROG_Y - 2 && my < topPos + PROG_Y + PROG_H + 10;
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
                    // optimistic client flip for snappy UI
                    orb.setAutoExport(!orb.isAutoExport());
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
        var orb = menu.getOrb();
        if (orb != null && orb.getRecipeEnergy() > 0) {
            g.drawString(font,
                    fmt(orb.getProgress()) + "/" + fmt(orb.getRecipeEnergy()),
                    leftPos + PROG_X, topPos + PROG_Y + PROG_H + 2, 0xFF373737, false);
        }

        // Advanced: show rod cache under progress
        if (orb instanceof AdvancedEnergizingOrbBlockEntity adv && adv.getDisplayCapacity() > 0) {
            String cache = "缓存 " + fmt(adv.getDisplayEnergy()) + "/" + fmt(adv.getDisplayCapacity())
                    + " " + adv.getEnergyUnit();
            g.drawString(font, cache, leftPos + 28, topPos + 70, 0xFF373737, false);
        }

        // Tooltips
        if (inBtn(mouseX, mouseY, guideX(), guideY())) {
            g.renderTooltip(font, Component.translatable("applied_powah.gui.guide"), mouseX, mouseY);
        } else if (inBtn(mouseX, mouseY, exportX(), exportY())) {
            boolean on = orb != null && orb.isAutoExport();
            g.renderTooltip(font, Component.translatable(
                    on ? "applied_powah.gui.auto_export.on" : "applied_powah.gui.auto_export.off"),
                    mouseX, mouseY);
        } else if (orb != null && orb.getRecipeEnergy() > 0 && hoverProgress(mouseX, mouseY)) {
            long prog = orb.getProgress();
            long max = orb.getRecipeEnergy();
            int pct = max <= 0 ? 0 : (int) Math.round(100.0 * prog / max);
            g.renderTooltip(font, Component.translatable(
                    "applied_powah.tooltip.progress",
                    fmt(prog), fmt(max), pct),
                    mouseX, mouseY);
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }
}
