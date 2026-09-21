package com.coala.appliedpowah.guide;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * In-game guide screen. Content is instructional and kept aligned with
 * docs/guidebook and AE2/Powah documentation tone (no colloquial phrasing).
 */
public class GuideBookScreen extends Screen {

    private int scroll = 0;

    public GuideBookScreen() {
        super(Component.translatable("item.applied_powah.guide_book"));
    }

    private static List<String> lines() {
        return List.of(
                "Applied Powah",
                "",
                "【概述】",
                "为 Applied Energistics 2 提供高密度能源元件，",
                "以及可从 ME 网络取能、向 Powah Energizing Orb",
                "供电的 AE / ME 充能棒。",
                "",
                "【能源元件】",
                "超密：12.8M AE；8×AE2 能源元件 + 运算处理器。",
                "极密：102.4M AE；8×超密 + 运算处理器。",
                "挖掘时保留内部 AE（与 AE2 元件一致）。",
                "",
                "【充能棒】",
                "完整方块，须邻接 AE2 线缆；facing 指向线缆。",
                "AE 棒：自 ME 网格抽取 AE。",
                "ME 棒：自 ME 网络抽取 FE（Applied Flux）。",
                "节点需持有频道方可取能；网格默认保留 5%。",
                "邻近存在含合法配方的 Powah Energizing Orb 时，",
                "棒会向其输出能量。",
                "",
                "【配方】",
                "AE 棒：十字 4×能源元件 + 中心同档 Powah 棒。",
                "ME 棒：四角 4×能源元件 + 中心同档 Powah 棒。",
                "升级：材料同 Powah 对应档，棒位换为上一档 AP 棒。",
                "AE 与 ME 同档可无序互转。",
                "缺少 Powah / Applied Flux 时对应配方自动禁用。",
                "",
                "【JEI】",
                "充能棒在工作台合成，不属于 Powah「充能」配方。",
                "安装 JEI 后，棒会出现在 Energizing 类目 catalyst 中。",
                "",
                "【配置】",
                "config/applied_powah-common.toml：",
                "pullIntervalTicks / networkReserveRatio",
                "aeBurstAe / meBurstFe",
                "",
                "【说明】",
                "物品 tooltip 不显示放置后的实时能量。",
                "AE2 GuideME（长按 G）尚未接入本模组页面。",
                "完整文稿见仓库 docs/guidebook/。"
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
