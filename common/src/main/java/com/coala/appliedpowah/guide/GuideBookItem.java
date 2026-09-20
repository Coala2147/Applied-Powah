package com.coala.appliedpowah.guide;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** In-game guide book (standalone; AE2 GuideME pages are separate). */
public class GuideBookItem extends Item {
    public GuideBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openGuide();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private static void openGuide() {
        try {
            net.minecraft.client.Minecraft.getInstance().setScreen(new GuideBookScreen());
        } catch (Throwable t) {
            AppliedPowah.LOG.warn("Failed to open guide screen: {}", t.toString());
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.applied_powah.guide_book");
    }
}
