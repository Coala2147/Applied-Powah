package com.coala.appliedpowah.meorb;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.coala.appliedpowah.orb.EnergizingOrbLogic;
import com.coala.appliedpowah.orb.OrbCraftGate;
import net.minecraft.world.item.ItemStack;

/**
 * ME 充能台样板仓 + CPU 握手（EAE 1.21.1 样板页）。
 *
 * 阻挡模式对齐 AE2 {@code PatternProviderLogic.pushPattern}：
 * isBlocking 且目标（此处 = 台输入槽 1..6）里已出现任一样板原料 key → 拒绝再投。
 */
public class MePatternLogic extends PatternProviderLogic {

    private final MeEnergizingOrbBlockEntity meOrb;

    public MePatternLogic(IManagedGridNode mainNode, PatternProviderLogicHost host,
            MeEnergizingOrbBlockEntity meOrb, int patternSlots) {
        super(mainNode, host, patternSlots);
        this.meOrb = meOrb;
    }

    @Override
    public boolean isBusy() {
        if (super.isBusy()) {
            return true;
        }
        if (meOrb == null || meOrb.getLevel() == null) {
            return false;
        }
        return meOrb.isCrafting() || meOrb.hasPendingProduct();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (patternDetails == null || inputHolder == null) {
            return false;
        }
        if (!holdsPattern(patternDetails)) {
            return false;
        }
        if (meOrb.getMainNode() == null || !meOrb.getMainNode().isActive()) {
            return false;
        }
        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        if (OrbCraftGate.blocksPush(isBlocking(), craftSlotsContainPatternInput())) {
            return false;
        }
        if (isBusy()) {
            return false;
        }
        return meOrb.acceptCpuPatternInputs(inputHolder);
    }

    private boolean craftSlotsContainPatternInput() {
        var inv = meOrb.getInv();
        int slots = Math.min(EnergizingOrbLogic.SLOTS, inv.getSlots());
        for (int slot = 1; slot < slots; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            AEItemKey itemKey = AEItemKey.of(stack);
            if (itemKey == null) {
                continue;
            }
            AEKey flat = itemKey.dropSecondary();
            for (IPatternDetails p : getAvailablePatterns()) {
                for (var input : p.getInputs()) {
                    for (var cand : input.getPossibleInputs()) {
                        if (cand.what().dropSecondary().equals(flat)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean holdsPattern(IPatternDetails patternDetails) {
        for (IPatternDetails p : getAvailablePatterns()) {
            if (p == patternDetails) {
                return true;
            }
            if (p.getDefinition() != null && p.getDefinition().equals(patternDetails.getDefinition())) {
                return true;
            }
        }
        return false;
    }
}
