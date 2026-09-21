package com.coala.appliedpowah.guide;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Creative-tab / inventory guide item; opens the GuideME book when GuideME is loaded. */
public final class APGuide {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, com.coala.appliedpowah.AppliedPowah.MOD_ID);

    public static final RegistryObject<Item> BOOK = ITEMS.register("guide_book",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    private APGuide() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
