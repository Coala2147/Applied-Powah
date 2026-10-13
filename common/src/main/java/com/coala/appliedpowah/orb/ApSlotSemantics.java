package com.coala.appliedpowah.orb;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/**
 * One-time registration of the slot semantics used by the multi-page orb menus.
 *
 * <p>AE2's {@link SlotSemantics#register(String, boolean)} throws on duplicate ids, so
 * every registration is guarded by {@link SlotSemantics#get(String)}. The ScreenStyle
 * JSONs reference these ids to place the slots, hence they must exist before any screen
 * style is loaded (client) — the mod constructor also touches this class early.</p>
 */
public final class ApSlotSemantics {

    /** One semantics per page: its 6 input slots share the page grid. */
    public static final SlotSemantic[] PAGE =
            new SlotSemantic[AdvancedEnergizingOrbBlockEntity.PAGES];
    /** Rod slots of Advanced / ME tiers (vertical column). */
    public static final SlotSemantic ROD = new SlotSemantic("ap_orb_rod", false);

    static {
        for (int i = 0; i < PAGE.length; i++) {
            PAGE[i] = register("ap_orb_page_" + (i + 1));
        }
    }

    private ApSlotSemantics() {
    }

    /** Forces class initialisation (idempotent). */
    public static void touch() {
        // no-op — the static initializer runs once
    }

    public static SlotSemantic page(int index) {
        return PAGE[Math.max(0, Math.min(PAGE.length - 1, index))];
    }

    private static SlotSemantic register(String id) {
        SlotSemantic existing = SlotSemantics.get(id);
        if (existing != null) {
            return existing;
        }
        return SlotSemantics.register(id, false);
    }
}
