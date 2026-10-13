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

/**
 * Registration for AP energizing orbs (requires AE2 + Powah at runtime).
 *
 * <p>0.4.0 registry ids:</p>
 * <ul>
 *   <li>{@code fluix_energizing_orb} 鈥?simple rod-fed orb (was {@code me_energizing_orb})</li>
 *   <li>{@code advanced_energizing_orb} 鈥?rod cache orb (unchanged)</li>
 *   <li>{@code me_energizing_orb} 鈥?pattern-provider ME orb (was {@code auto_energizing_orb})</li>
 * </ul>
 */
public final class APOrbs {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AppliedPowah.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AppliedPowah.MOD_ID);

    private static final AtomicReference<BlockEntityType<EnergizingOrbBlockEntity>> FLUIX_TYPE_REF =
            new AtomicReference<>();
    private static final AtomicReference<BlockEntityType<AdvancedEnergizingOrbBlockEntity>> ADV_TYPE_REF =
            new AtomicReference<>();
    private static final AtomicReference<BlockEntityType<com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity>> ME_TYPE_REF =
            new AtomicReference<>();

    public static final RegistryObject<Block> FLUIX_ORB = BLOCKS.register(
            "fluix_energizing_orb", FluixEnergizingOrbBlock::new);
    public static final RegistryObject<Block> ADV_ORB = BLOCKS.register(
            "advanced_energizing_orb", AdvancedEnergizingOrbBlock::new);
    public static final RegistryObject<Block> ME_ORB = BLOCKS.register(
            "me_energizing_orb", com.coala.appliedpowah.meorb.MeEnergizingOrbBlock::new);

    public static final RegistryObject<Item> FLUIX_ORB_ITEM = ITEMS.register(
            "fluix_energizing_orb", () -> new BlockItem(FLUIX_ORB.get(), new Item.Properties()));
    public static final RegistryObject<Item> ADV_ORB_ITEM = ITEMS.register(
            "advanced_energizing_orb", () -> new BlockItem(ADV_ORB.get(), new Item.Properties()));
    public static final RegistryObject<Item> ME_ORB_ITEM = ITEMS.register(
            "me_energizing_orb", () -> new BlockItem(ME_ORB.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<?>> FLUIX_ORB_TYPE =
            BLOCK_ENTITIES.register("fluix_energizing_orb", () -> {
                BlockEntityType<EnergizingOrbBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new EnergizingOrbBlockEntity(FLUIX_TYPE_REF.get(), pos, state),
                                FLUIX_ORB.get())
                        .build(null);
                FLUIX_TYPE_REF.set(type);
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

    public static final RegistryObject<BlockEntityType<?>> ME_ORB_TYPE =
            BLOCK_ENTITIES.register("me_energizing_orb", () -> {
                BlockEntityType<com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity(ME_TYPE_REF.get(), pos, state),
                                ME_ORB.get())
                        .build(null);
                ME_TYPE_REF.set(type);
                return type;
            });

    @SuppressWarnings("unchecked")
    public static BlockEntityType<EnergizingOrbBlockEntity> fluixType() {
        return (BlockEntityType<EnergizingOrbBlockEntity>) (BlockEntityType<?>) FLUIX_TYPE_REF.get();
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<AdvancedEnergizingOrbBlockEntity> advType() {
        return (BlockEntityType<AdvancedEnergizingOrbBlockEntity>) (BlockEntityType<?>) ADV_TYPE_REF.get();
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity> meType() {
        return (BlockEntityType<com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity>) (BlockEntityType<?>) ME_TYPE_REF.get();
    }

    /**
     * One MenuType per tier — the tier decides the ScreenStyle JSON (page count,
     * rod slots, pattern button), and the client factory dispatches on the BE type.
     */
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<EnergizingOrbMenu>> FLUIX_ORB_MENU =
            MENUS.register("fluix_energizing_orb", () ->
                    net.minecraftforge.common.extensions.IForgeMenuType.create(
                            (id, inv, buf) -> EnergizingOrbMenu.clientFactory(
                                    EnergizingOrbMenu.Tier.FLUIX, id, inv, buf)));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<EnergizingOrbMenu>> ADV_ORB_MENU =
            MENUS.register("advanced_energizing_orb", () ->
                    net.minecraftforge.common.extensions.IForgeMenuType.create(
                            (id, inv, buf) -> EnergizingOrbMenu.clientFactory(
                                    EnergizingOrbMenu.Tier.ADVANCED, id, inv, buf)));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<EnergizingOrbMenu>> ME_ORB_MENU =
            MENUS.register("me_energizing_orb", () ->
                    net.minecraftforge.common.extensions.IForgeMenuType.create(
                            (id, inv, buf) -> EnergizingOrbMenu.clientFactory(
                                    EnergizingOrbMenu.Tier.ME, id, inv, buf)));

    private static final java.util.Set<String> REMOVED_CARD_ITEMS = java.util.Set.of(
            "thread_card_p1", "thread_card_p2", "thread_card_p4", "thread_card_p8",
            "parallel_card_x2", "parallel_card_x4", "parallel_card_x6", "parallel_card_x8");

    private APOrbs() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(APOrbs::onCommonSetup);
        bus.addListener(APOrbs::onRegisterMenuTypes);
        // MissingMappingsEvent is a GAME-bus event (IModBusEvent check fails on mod bus).
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(APOrbs::onMissingMappings);
        AppliedPowah.LOG.info("Registered AP energizing orbs (fluix / advanced / me)");
    }

    /**
     * MenuTypeBuilder.build only queues into AE2 InitMenuTypes. AE2 (loads first)
     * flushes that queue during its own MENU RegisterEvent 鈥?before our TYPE
     * exists. Mirror ExtendedAE and register explicitly here, otherwise the
     * pattern page cannot open.
     */
    private static void onRegisterMenuTypes(net.minecraftforge.registries.RegisterEvent event) {
        if (!event.getRegistryKey().equals(net.minecraft.core.registries.Registries.MENU)) {
            return;
        }
        try {
            var id = appeng.core.AppEng.makeId("me_energizing_orb_patterns");
            var type = com.coala.appliedpowah.meorb.MePatternMenu.TYPE;
            ForgeRegistries.MENU_TYPES.register(id, type);
            AppliedPowah.LOG.info("Registered menu type {}", id);
        } catch (Throwable t) {
            AppliedPowah.LOG.error("Failed to register me_energizing_orb_patterns menu type", t);
        }
    }

    /**
     * 0.4.0 id migration: old {@code auto_energizing_orb} 鈫?new {@code me_energizing_orb}.
     * (Old {@code me_energizing_orb} now resolves to the new ME orb 鈥?intentional
     * breaking change of this major rename; recreate Fluix orbs if needed.)
     */
    private static void onMissingMappings(
            net.minecraftforge.registries.MissingMappingsEvent event) {
        try {
            for (var m : event.<Block>getMappings(
                    net.minecraft.core.registries.Registries.BLOCK, AppliedPowah.MOD_ID)) {
                if ("auto_energizing_orb".equals(m.getKey().getPath()) && ME_ORB.isPresent()) {
                    m.remap(ME_ORB.get());
                    AppliedPowah.LOG.info("Remapped block auto_energizing_orb -> me_energizing_orb");
                }
            }
            for (var m : event.<Item>getMappings(
                    net.minecraft.core.registries.Registries.ITEM, AppliedPowah.MOD_ID)) {
                String path = m.getKey().getPath();
                if ("auto_energizing_orb".equals(path) && ME_ORB_ITEM.isPresent()) {
                    m.remap(ME_ORB_ITEM.get());
                    AppliedPowah.LOG.info("Remapped item auto_energizing_orb -> me_energizing_orb");
                } else if (REMOVED_CARD_ITEMS.contains(path)) {
                    // 0.6.2: thread/parallel cards replaced by AE2 speed/energy cards
                    m.ignore();
                    AppliedPowah.LOG.info("Dropped removed upgrade card item {}", path);
                }
            }
            for (var m : event.<BlockEntityType<?>>getMappings(
                    net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, AppliedPowah.MOD_ID)) {
                if ("auto_energizing_orb".equals(m.getKey().getPath())) {
                    var t = meType();
                    if (t != null) {
                        m.remap(t);
                        AppliedPowah.LOG.info("Remapped BE auto_energizing_orb -> me_energizing_orb");
                    }
                }
            }
        } catch (Throwable t) {
            AppliedPowah.LOG.error("MissingMappings remap failed", t);
        }
    }

    /**
     * AE2 {@code AEBaseEntityBlock} requires {@code setBlockEntity} or
     * {@code getBlockEntityBlockState} NPEs on first tick.
     */
    private static void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                appeng.block.AEBaseEntityBlock fluixBlock = (appeng.block.AEBaseEntityBlock) FLUIX_ORB.get();
                fluixBlock.setBlockEntity(EnergizingOrbBlockEntity.class, fluixType(), null, null);

                @SuppressWarnings({"unchecked", "rawtypes"})
                appeng.block.AEBaseEntityBlock advBlock = (appeng.block.AEBaseEntityBlock) ADV_ORB.get();
                advBlock.setBlockEntity(AdvancedEnergizingOrbBlockEntity.class, advType(), null, null);

                @SuppressWarnings({"unchecked", "rawtypes"})
                appeng.block.AEBaseEntityBlock meBlock = (appeng.block.AEBaseEntityBlock) ME_ORB.get();
                meBlock.setBlockEntity(com.coala.appliedpowah.meorb.MeEnergizingOrbBlockEntity.class, meType(), null, null);

                AppliedPowah.LOG.info("Bound orb block entities to AEBaseEntityBlock");
            } catch (Throwable t) {
                AppliedPowah.LOG.error("Failed to bind orb block entities", t);
            }
        });
    }
}
