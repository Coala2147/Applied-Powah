package com.coala.appliedpowah.orb;

import com.coala.appliedpowah.chargingrod.RodBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Page-aware orb slots.
 *
 * <p>Every page's slots occupy the same GUI coordinates, so only the active page may be
 * drawn and clicked: {@link #isActive()} is false for inactive pages, which hides both
 * the item and the hover highlight (vanilla checks {@code isActive()} in
 * {@code renderSlot} and {@code findSlot}).</p>
 */
abstract class OrbSlot extends SlotItemHandler {

    private final int page;
    private boolean pageActive = true;

    OrbSlot(ItemStackHandler handler, int index, int page) {
        super(handler, index, 0, 0);
        this.page = page;
    }

    final int page() {
        return page;
    }

    final void setPageActive(boolean active) {
        this.pageActive = active;
    }

    /** Hidden pages are inert: no item rendering, no clicks. */
    @Override
    public final boolean isActive() {
        return pageActive;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isActive() && super.mayPlace(stack);
    }
}

/** Craft input slot of one page (limit 1 item, no rods — the handler enforces both). */
final class OrbInputSlot extends OrbSlot {

    OrbInputSlot(ItemStackHandler handler, int index, int page) {
        super(handler, index, page);
    }
}

/** Craft output slot of one page (read-only for players). */
final class OrbOutputSlot extends OrbSlot {

    OrbOutputSlot(ItemStackHandler handler, int index, int page) {
        super(handler, index, page);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}

/** Rod slot (Advanced / ME tiers) — only AP rods, one family per machine. */
final class OrbRodSlot extends SlotItemHandler {

    OrbRodSlot(ItemStackHandler handler, int index) {
        super(handler, index, 0, 0);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof RodBlockItem && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize() {
        return AdvancedEnergizingOrbBlockEntity.MAX_RODS_PER_SLOT;
    }
}
