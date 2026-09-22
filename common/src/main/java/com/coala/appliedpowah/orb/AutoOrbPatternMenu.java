package com.coala.appliedpowah.orb;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * Auto-orb pattern menu — mirrors ExtendedAE {@code ContainerExPatternProvider}:
 * plain {@link PatternProviderMenu} + own MenuType so AE2 loads
 * {@code auto_orb_patterns.json} (BREAK_AFTER_9COLS → 4×9).
 */
public class AutoOrbPatternMenu extends PatternProviderMenu {

    public static final MenuType<AutoOrbPatternMenu> TYPE = MenuTypeBuilder
            .create(AutoOrbPatternMenu::new, PatternProviderLogicHost.class)
            .build("auto_orb_patterns");

    protected AutoOrbPatternMenu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
        super(TYPE, id, playerInventory, host);
    }
}
