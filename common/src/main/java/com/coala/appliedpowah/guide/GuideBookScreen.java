package com.coala.appliedpowah.guide;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Simple scrollable guide screen (content from GuideBookScreen.LINES / lang). */
public class GuideBookScreen extends Screen {

    private int scroll = 0;

    public GuideBookScreen() {
        super(Component.translatable("item.applied_powah.guide_book"));
    }

    private static List<String> lines() {
        return List.of(
                "Applied Powah — 指南",
                "",
                "【能源元件】",
                "超密 = 8×AE2致密 + 运算处理器",
                "极密 = 8×超密 + 运算处理器",
                "挖掘后保留内部 AE（与 AE2 元件相同）。",
                "",
                "【AE / ME 充能棒】",
                "完整方块，不是线缆小零件。",
                "必须放在 AE2 线缆旁；facing 朝向线缆。",
                "AE 棒：从 ME 网络抽 AE；ME 棒：抽 Applied Flux FE。",
                "靠近 Powah 充能台时自动喂电（有光柱）。",
                "需要频道才工作；网络保留约 5% AE。",
                "",
                "【配方】",
                "AE 棒：十字四格能源元件 + 中心同档 Powah 棒。",
                "ME 棒：四角能源元件 + 中心同档 Powah 棒。",
                "升级：仿 Powah，棒位换成上一档 AP 棒。",
                "AE↔ME：同档无序互转。",
                "",
                "【JEI】",
                "AP 棒由工作台合成，不在 Powah「充能」页。",
                "在 JEI 中对 AP 棒按 R 应显示工作台配方。",
                "",
                "【关于长按 G（AE2 GuideME）】",
                "G 打开的是 AE2 手册；本 mod 内容请用手持「Applied Powah 指南」。",
                "完整图文见仓库 docs/guidebook/。",
                "",
                "配置：pullIntervalTicks / networkReserveRatio / aeBurstAe / meBurstFe"
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        int w = 280;
        int x = (this.width - w) / 2;
        int y = 40;
        g.fill(x - 8, y - 8, x + w + 8, this.height - 30, 0xE0101018);
        List<String> ls = lines();
        int maxScroll = Math.max(0, ls.size() * 12 - (this.height - 80));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int yy = y - scroll;
        for (String line : ls) {
            if (yy > y - 20 && yy < this.height - 40) {
                g.drawCenteredString(this.font, line, this.width / 2, yy, 0xFFE0E0E0);
            }
            yy += 12;
        }
        g.drawCenteredString(this.font, Component.translatable("gui.applied_powah.guide_scroll"), this.width / 2, this.height - 22, 0xFF808080);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll -= (int) (delta * 12);
        List<String> ls = lines();
        int maxScroll = Math.max(0, ls.size() * 12 - (this.height - 80));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
