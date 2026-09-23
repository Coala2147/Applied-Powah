package com.coala.appliedpowah.meorb;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * ME 充能台样板页菜单 — EAE 1.21.1 ContainerExPatternProvider 对应。
 */
public class MePatternMenu extends PatternProviderMenu {

    public static final MenuType<MePatternMenu> TYPE = MenuTypeBuilder
            .create(MePatternMenu::new, PatternProviderLogicHost.class)
            .build("me_energizing_orb_patterns");

    public MePatternMenu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
        super(TYPE, id, playerInventory, host);
    }
}
