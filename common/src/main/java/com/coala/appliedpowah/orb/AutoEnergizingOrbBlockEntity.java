package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Auto Energizing Orb.
 * Advanced orb + 36 pattern slots (4×9) acting as a Pattern Provider.
 * Works like an Advanced orb for energizing, while also exposing patterns to the ME network.
 */
public class AutoEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;

    private final PatternProviderLogic logic = new PatternProviderLogic(getMainNode(), this, PATTERN_SLOTS);

    public AutoEnergizingOrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onMainNodeStateChanged(appeng.api.networking.IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        this.logic.onMainNodeStateChanged();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.logic.updatePatterns();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.logic.writeToNBT(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        this.logic.readFromNBT(tag);
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        this.logic.clearContent();
    }

    @Override
    public int getPriority() {
        return logic.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        logic.setPriority(priority);
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(APOrbs.AUTO_ORB_ITEM.get()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(APOrbs.AUTO_ORB_ITEM.get());
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            logic.exportSettings(output);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            logic.importSettings(input, player);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        var lo = logic.getCapability(cap);
        if (lo.isPresent()) {
            return lo;
        }
        return super.getCapability(cap, side);
    }

    public void openPatternMenu(Player player, MenuLocator locator) {
        appeng.menu.MenuOpener.open(PatternProviderMenu.TYPE, player, locator);
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.NO_ACCESS;
    }
}
