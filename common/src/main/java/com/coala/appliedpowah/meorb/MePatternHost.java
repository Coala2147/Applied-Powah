package com.coala.appliedpowah.meorb;

import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.locator.MenuLocator;
import com.coala.appliedpowah.orb.APOrbs;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * PatternProvider host that is <b>not</b> the BlockEntity.
 *
 * <p>AE2 {@code PatternProviderLogic#getActiveSides} drops any face that has a grid
 * connection to another {@code PatternProviderLogicHost}. If the ME orb itself
 * implements that interface, an external PatternProvider stacked under it will
 * refuse to push patterns. This delegate keeps the interface off the BE class
 * while still hosting {@link MePatternLogic}.</p>
 */
final class MePatternHost implements PatternProviderLogicHost {

    private final MeEnergizingOrbBlockEntity orb;
    private PatternProviderLogic logic;

    MePatternHost(MeEnergizingOrbBlockEntity orb, PatternProviderLogic logic) {
        this.orb = orb;
        this.logic = logic;
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic != null ? logic : orb.getLogic();
    }

    @Override
    public BlockEntity getBlockEntity() {
        return orb;
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public void saveChanges() {
        orb.saveChangesFromPattern();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(APOrbs.ME_ORB_ITEM.get()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(APOrbs.ME_ORB_ITEM.get());
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        orb.openPatternMenu(player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, appeng.menu.ISubMenu subMenu) {
        orb.openOrbMenu((net.minecraft.server.level.ServerPlayer) player);
    }
}
