package com.coala.appliedpowah.orb;

import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Auto-orb pattern screen — mirrors ExtendedAE {@code GuiExPatternProvider}:
 * just {@link PatternProviderScreen} + our ScreenStyle JSON
 * ({@code /screens/auto_orb_patterns.json}).
 */
public class AutoOrbPatternScreen extends PatternProviderScreen<AutoOrbPatternMenu> {

    public AutoOrbPatternScreen(AutoOrbPatternMenu menu, Inventory playerInventory,
                               Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
