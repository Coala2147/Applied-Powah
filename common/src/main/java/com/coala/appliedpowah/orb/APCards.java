package com.coala.appliedpowah.orb;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Thread / parallel upgrade cards (8 items). Energy card = AE2 vanilla
 * {@code ae2:energy_card} (Q10: not re-registered here).
 */
public final class APCards {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPowah.MOD_ID);

    public static final RegistryObject<Item> THREAD_P1 = ITEMS.register(
            "thread_card_p1", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.THREAD, 1, new Item.Properties()));
    public static final RegistryObject<Item> THREAD_P2 = ITEMS.register(
            "thread_card_p2", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.THREAD, 2, new Item.Properties()));
    public static final RegistryObject<Item> THREAD_P4 = ITEMS.register(
            "thread_card_p4", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.THREAD, 4, new Item.Properties()));
    public static final RegistryObject<Item> THREAD_P8 = ITEMS.register(
            "thread_card_p8", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.THREAD, 8, new Item.Properties()));

    public static final RegistryObject<Item> PARALLEL_X2 = ITEMS.register(
            "parallel_card_x2", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.PARALLEL, 2, new Item.Properties()));
    public static final RegistryObject<Item> PARALLEL_X4 = ITEMS.register(
            "parallel_card_x4", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.PARALLEL, 4, new Item.Properties()));
    public static final RegistryObject<Item> PARALLEL_X6 = ITEMS.register(
            "parallel_card_x6", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.PARALLEL, 6, new Item.Properties()));
    public static final RegistryObject<Item> PARALLEL_X8 = ITEMS.register(
            "parallel_card_x8", () -> new UpgradeCardItem(
                    UpgradeCardItem.Kind.PARALLEL, 8, new Item.Properties()));

    private APCards() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
