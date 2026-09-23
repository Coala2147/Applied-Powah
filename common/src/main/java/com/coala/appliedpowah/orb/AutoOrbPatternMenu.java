package com.coala.appliedpowah.orb;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * ME orb pattern menu — ExtendedAE {@code ContainerExPatternProvider} clone.
 * 36 pattern slots come from {@code logic.getPatternInv().size()} in the parent.
 */
public class AutoOrbPatternMenu extends PatternProviderMenu {

    public static final MenuType<AutoOrbPatternMenu> TYPE = MenuTypeBuilder
            .create(AutoOrbPatternMenu::new, PatternProviderLogicHost.class)
            .build("auto_orb_patterns");

    public AutoOrbPatternMenu(int id, Inventory playerInventory, PatternProviderLogicHost host) {
        super(TYPE, id, playerInventory, host);
    }
}
