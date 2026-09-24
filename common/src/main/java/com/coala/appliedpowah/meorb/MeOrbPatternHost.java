package com.coala.appliedpowah.meorb;

import appeng.api.implementations.blockentities.PatternContainerGroup;
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
 * AE2 {@link PatternProviderLogicHost} view of the ME orb.
 *
 * <p>Intentionally <b>not</b> implemented on the BlockEntity (Mek-Energistics
 * pattern). {@code PatternProviderLogic#getActiveSides} skips faces whose grid
 * neighbor owner is a {@code PatternProviderLogicHost} — that made an external
 * PatternProvider refuse to push into this orb. The grid node owner is the BE,
 * so the BE must not implement that interface. Menu binding uses
 * {@code MeEnergizingOrbBlockEntity.class} and wraps via {@link #of}.</p>
 */
final class MeOrbPatternHost implements PatternProviderLogicHost {

    private final MeEnergizingOrbBlockEntity orb;
    private PatternProviderLogic logic;

    private MeOrbPatternHost(MeEnergizingOrbBlockEntity orb, PatternProviderLogic logic) {
        this.orb = orb;
        this.logic = logic;
    }

    static MeOrbPatternHost of(MeEnergizingOrbBlockEntity orb, PatternProviderLogic logic) {
        return new MeOrbPatternHost(orb, logic);
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
        orb.setChanged();
        orb.markForUpdate();
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
