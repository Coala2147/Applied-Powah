package com.coala.appliedpowah.meorb;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import com.coala.appliedpowah.network.APNetwork;
import com.coala.appliedpowah.network.C2SOpenOrbMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * ME 充能台样板页 — EAE 1.21.1 GuiExPatternProvider 对应。
 * 左侧工具栏首项：返回 ME 台 GUI。
 */
public class MePatternScreen extends PatternProviderScreen<MePatternMenu> {

    public MePatternScreen(MePatternMenu menu, Inventory playerInventory,
                           Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.addToLeftToolbar(new IconButton(btn -> {
            var orb = menu.getMeOrb();
            if (orb != null) {
                APNetwork.sendToServer(new C2SOpenOrbMenu(orb.getBlockPos()));
            }
        }) {
            @Override
            protected Icon getIcon() {
                return Icon.ARROW_LEFT;
            }

            @Override
            public Component getMessage() {
                return Component.translatable("applied_powah.gui.back_to_orb");
            }
        });
    }
}
