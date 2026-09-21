package com.coala.appliedpowah.orb;

import com.coala.appliedpowah.AppliedPowah;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.concurrent.atomic.AtomicReference;

/** Registration for AP energizing orbs (requires AE2 + Powah at runtime). */
public final class APOrbs {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AppliedPowah.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AppliedPowah.MOD_ID);

    private static final AtomicReference<BlockEntityType<MeEnergizingOrbBlockEntity>> ME_TYPE_REF =
            new AtomicReference<>();
    private static final AtomicReference<BlockEntityType<AdvancedEnergizingOrbBlockEntity>> ADV_TYPE_REF =
            new AtomicReference<>();

    public static final RegistryObject<Block> ME_ORB = BLOCKS.register(
            "me_energizing_orb", MeEnergizingOrbBlock::new);
    public static final RegistryObject<Block> ADV_ORB = BLOCKS.register(
            "advanced_energizing_orb", AdvancedEnergizingOrbBlock::new);

    public static final RegistryObject<Item> ME_ORB_ITEM = ITEMS.register(
            "me_energizing_orb", () -> new BlockItem(ME_ORB.get(), new Item.Properties()));
    public static final RegistryObject<Item> ADV_ORB_ITEM = ITEMS.register(
            "advanced_energizing_orb", () -> new BlockItem(ADV_ORB.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<?>> ME_ORB_TYPE =
            BLOCK_ENTITIES.register("me_energizing_orb", () -> {
                BlockEntityType<MeEnergizingOrbBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new MeEnergizingOrbBlockEntity(ME_TYPE_REF.get(), pos, state),
                                ME_ORB.get())
                        .build(null);
                ME_TYPE_REF.set(type);
                return type;
            });

    public static final RegistryObject<BlockEntityType<?>> ADV_ORB_TYPE =
            BLOCK_ENTITIES.register("advanced_energizing_orb", () -> {
                BlockEntityType<AdvancedEnergizingOrbBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new AdvancedEnergizingOrbBlockEntity(ADV_TYPE_REF.get(), pos, state),
                                ADV_ORB.get())
                        .build(null);
                ADV_TYPE_REF.set(type);
                return type;
            });

    @SuppressWarnings("unchecked")
    public static BlockEntityType<MeEnergizingOrbBlockEntity> meType() {
        return (BlockEntityType<MeEnergizingOrbBlockEntity>) (BlockEntityType<?>) ME_TYPE_REF.get();
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<AdvancedEnergizingOrbBlockEntity> advType() {
        return (BlockEntityType<AdvancedEnergizingOrbBlockEntity>) (BlockEntityType<?>) ADV_TYPE_REF.get();
    }

    public static final RegistryObject<net.minecraft.world.inventory.MenuType<EnergizingOrbMenu>> ORB_MENU =
            MENUS.register("energizing_orb", () ->
                    net.minecraftforge.common.extensions.IForgeMenuType.create(EnergizingOrbMenu::clientFactory));

    private APOrbs() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(APOrbs::onCommonSetup);
        AppliedPowah.LOG.info("Registered AP energizing orbs");
    }

    /**
     * AE2 {@code AEBaseEntityBlock} requires {@code setBlockEntity} or
     * {@code getBlockEntityBlockState} NPEs on first tick (crash: blockEntityClass null).
     */
    private static void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                appeng.block.AEBaseEntityBlock meBlock = (appeng.block.AEBaseEntityBlock) ME_ORB.get();
                meBlock.setBlockEntity(MeEnergizingOrbBlockEntity.class, meType(), null, null);

                @SuppressWarnings({"unchecked", "rawtypes"})
                appeng.block.AEBaseEntityBlock advBlock = (appeng.block.AEBaseEntityBlock) ADV_ORB.get();
                advBlock.setBlockEntity(AdvancedEnergizingOrbBlockEntity.class, advType(), null, null);

                AppliedPowah.LOG.info("Bound orb block entities to AEBaseEntityBlock");
            } catch (Throwable t) {
                AppliedPowah.LOG.error("Failed to bind orb block entities", t);
            }
        });
    }
}
