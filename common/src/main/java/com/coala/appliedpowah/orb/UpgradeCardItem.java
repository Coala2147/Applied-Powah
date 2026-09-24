package com.coala.appliedpowah.orb;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Upgrade card kinds for the 6-slot universal upgrade inventory.
 * Thread / parallel cards stack to 1; energy cards are AE2 {@code ae2:energy_card}.
 */
public class UpgradeCardItem extends Item {

    public enum Kind {
        THREAD, PARALLEL
    }

    private final Kind kind;
    /** Thread: bonus T (1/2/4/8). Parallel: multiplier (2/4/6/8). */
    private final int value;

    public UpgradeCardItem(Kind kind, int value, Properties properties) {
        super(properties);
        this.kind = kind;
        this.value = value;
    }

    public Kind getKind() {
        return kind;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // AE2 UpgradeCardItem: "Supported by:" then machine names (Tooltips.NORMAL_TOOLTIP_TEXT)
        tooltip.add(net.minecraft.network.chat.Component.translatable("gui.tooltips.ae2.SupportedBy")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        for (var key : new String[] {
                "block.applied_powah.fluix_energizing_orb",
                "block.applied_powah.advanced_energizing_orb",
                "block.applied_powah.me_energizing_orb"}) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(key)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    public static boolean isThreadCard(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof UpgradeCardItem c && c.kind == Kind.THREAD;
    }

    public static boolean isParallelCard(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof UpgradeCardItem c && c.kind == Kind.PARALLEL;
    }

    /**
     * Energy card = AE2 vanilla {@code ae2:energy_card}. Checked by registry name so
     * we do not need a hard class dependency at item-construction time.
     */
    public static boolean isEnergyCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "ae2".equals(key.getNamespace()) && "energy_card".equals(key.getPath());
    }

    public static boolean isUpgradeCard(ItemStack stack) {
        return isThreadCard(stack) || isParallelCard(stack) || isEnergyCard(stack);
    }
}
