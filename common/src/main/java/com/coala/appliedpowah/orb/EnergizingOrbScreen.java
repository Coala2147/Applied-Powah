package com.coala.appliedpowah.orb;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Placeholder GUI. Progress fill only while a recipe is active.
 * Hovering the progress bar shows stored/progress text (Powah-style).
 * World-space energy is AE2 Jade via IAEPowerStorage — do not build a custom Jade HUD.
 */
public class EnergizingOrbScreen extends AbstractContainerScreen<EnergizingOrbMenu> {

    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    private static final int PROG_X = 88;
    private static final int PROG_Y = 44;
    private static final int PROG_W = 16;
    private static final int PROG_H = 6;

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

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF8B8B8B);
        g.fill(x + 28, y + 12, x + 82, y + 66, 0xFF373737);
        g.fill(x + 30, y + 14, x + 80, y + 64, 0xFF8B8B8B);
        g.fill(x + 110, y + 30, x + 130, y + 50, 0xFF373737);
        g.fill(x + 112, y + 32, x + 128, y + 48, 0xFF8B8B8B);
        g.fill(x + 88, y + 36, x + 104, y + 42, 0xFF373737);

        var orb = menu.getOrb();
        long prog = orb == null ? 0 : orb.getProgress();
        long max = orb == null ? 0 : orb.getRecipeEnergy();
        // 无反应时不画进度填充
        if (max > 0) {
            int barX = x + PROG_X;
            int barY = y + PROG_Y;
            g.fill(barX, barY, barX + PROG_W, barY + PROG_H, 0xFF373737);
            int fill = (int) Math.min(PROG_W, (prog * PROG_W) / max);
            g.fill(barX, barY, barX + fill, barY + PROG_H, 0xFF00AA00);
        }

        if (orb instanceof AdvancedEnergizingOrbBlockEntity) {
            g.fill(x + 140, y + 12, x + 168, y + 84, 0xFF373737);
        }
        g.fill(x + 7, y + 83, x + 169, y + 163, 0xFF8B8B8B);
        if (orb != null && orb.isAutoExport()) {
            g.fill(x + 4, y + 4, x + 10, y + 10, 0xFF00C853);
        } else if (orb != null) {
            g.fill(x + 4, y + 4, x + 10, y + 10, 0xFFC62828);
        }
    }

    private boolean hoverProgress(int mx, int my) {
        return mx >= leftPos + PROG_X && mx < leftPos + PROG_X + PROG_W
                && my >= topPos + PROG_Y - 2 && my < topPos + PROG_Y + PROG_H + 10;
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

        if (orb != null && orb.getRecipeEnergy() > 0 && hoverProgress(mouseX, mouseY)) {
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
