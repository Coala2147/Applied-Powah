package com.coala.appliedpowah.meorb;

import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * ME 充能台样板页 — EAE 1.21.1 GuiExPatternProvider 对应。
 */
public class MePatternScreen extends PatternProviderScreen<MePatternMenu> {

    public MePatternScreen(MePatternMenu menu, Inventory playerInventory,
                           Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
