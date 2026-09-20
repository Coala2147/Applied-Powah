package com.coala.appliedpowah.guide;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class APGuide {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPowah.MOD_ID);

    public static final RegistryObject<Item> BOOK = ITEMS.register("guide_book",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    private APGuide() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
