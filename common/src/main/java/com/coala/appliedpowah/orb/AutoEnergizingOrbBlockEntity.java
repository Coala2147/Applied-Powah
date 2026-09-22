package com.coala.appliedpowah.orb;

import appeng.api.config.AccessRestriction;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

/**
 * Auto Energizing Orb.
 * Advanced orb + 36 pattern slots (4x9) acting as a Pattern Provider.
 * Pattern inputs are routed directly into the orb's own input slots instead of adjacent blocks.
 */
public class AutoEnergizingOrbBlockEntity extends AdvancedEnergizingOrbBlockEntity implements PatternProviderLogicHost {

    public static final int PATTERN_SLOTS = 36;

    private final PatternProviderLogic logic = new AutoOrbPatternProviderLogic(getMainNode(), this, PATTERN_SLOTS);

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
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public appeng.api.stacks.AEItemKey getTerminalIcon() {
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

    /**
     * Accept pattern inputs directly into the orb's input slots (slots 1-6).
     * Each slot can hold at most 1 item.
     */
    public boolean acceptPatternInputs(KeyCounter[] inputHolder) {
        if (inputHolder == null || inputHolder.length == 0) {
            return false;
        }

        // Collect all required item inputs
        List<Object2LongMap.Entry<AEKey>> inputs = new ArrayList<>();
        for (KeyCounter counter : inputHolder) {
            if (counter == null || counter.isEmpty()) {
                continue;
            }
            for (var entry : counter) {
                inputs.add(entry);
            }
        }

        if (inputs.isEmpty()) {
            return false;
        }

        // Check that we have enough empty input slots
        int emptySlots = 0;
        for (int i = 1; i < inv.getSlots() && i < EnergizingOrbLogic.SLOTS; i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                emptySlots++;
            }
        }
        if (emptySlots < inputs.size()) {
            return false;
        }

        // Insert items into empty input slots
        int slot = 1;
        for (var entry : inputs) {
            AEKey key = entry.getKey();
            long amount = entry.getLongValue();
            if (!(key instanceof AEItemKey itemKey) || amount <= 0) {
                continue;
            }
            while (slot < inv.getSlots() && slot < EnergizingOrbLogic.SLOTS) {
                if (inv.getStackInSlot(slot).isEmpty()) {
                    ItemStack stack = itemKey.toStack((int) Math.min(amount, Integer.MAX_VALUE));
                    inv.setStackInSlot(slot, stack);
                    slot++;
                    break;
                }
                slot++;
            }
        }

        return true;
    }
}
