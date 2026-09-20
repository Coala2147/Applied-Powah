package com.coala.appliedpowah.energycell;

import appeng.blockentity.networking.EnergyCellBlockEntity;
import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.chargingrod.EnergizingRodBlock;
import com.coala.appliedpowah.chargingrod.EnergizingRodBlockEntity;
import com.coala.appliedpowah.chargingrod.RodBlockItem;
import com.coala.appliedpowah.chargingrod.RodTier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Energy cells + full-block energizing rods. Only invoked when AE2 is loaded.
 * Rod registry names keep ae_energizing_rod_* / me_energizing_rod_* for existing recipes.
 */
public final class APCells {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AppliedPowah.MOD_ID);

    private static final AtomicReference<BlockEntityType<EnergyCellBlockEntity>> TYPE_REF = new AtomicReference<>();
    public static final AtomicReference<BlockEntityType<EnergizingRodBlockEntity>> ROD_TYPE_REF = new AtomicReference<>();

    public static final Map<RodTier, RegistryObject<Block>> AE_RODS = new EnumMap<>(RodTier.class);
    public static final Map<RodTier, RegistryObject<Block>> ME_RODS = new EnumMap<>(RodTier.class);

    static {
        for (RodTier tier : RodTier.values()) {
            AE_RODS.put(tier, BLOCKS.register(tier.aeRodId(),
                    () -> new EnergizingRodBlock(tier, true)));
            ME_RODS.put(tier, BLOCKS.register(tier.meRodId(),
                    () -> new EnergizingRodBlock(tier, false)));
        }
        for (RodTier tier : RodTier.values()) {
            ITEMS.register(tier.aeRodId(), () -> new RodBlockItem(AE_RODS.get(tier).get(), new Item.Properties(), tier, true));
            ITEMS.register(tier.meRodId(), () -> new RodBlockItem(ME_RODS.get(tier).get(), new Item.Properties(), tier, false));
        }
    }

    public static final RegistryObject<Block> SUPER_DENSE = BLOCKS.register(
            "super_dense_energy_cell", SuperDenseEnergyCellBlock::new);
    public static final RegistryObject<Block> EXTREME_DENSE = BLOCKS.register(
            "extreme_dense_energy_cell", ExtremeDenseEnergyCellBlock::new);

    public static final RegistryObject<Item> SUPER_DENSE_ITEM = ITEMS.register(
            "super_dense_energy_cell", () -> new APEnergyCellBlockItem(SUPER_DENSE.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXTREME_DENSE_ITEM = ITEMS.register(
            "extreme_dense_energy_cell", () -> new APEnergyCellBlockItem(EXTREME_DENSE.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELLS =
            BLOCK_ENTITIES.register("energy_cells", () -> {
                BlockEntityType<EnergyCellBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new EnergyCellBlockEntity(TYPE_REF.get(), pos, state),
                                SUPER_DENSE.get(), EXTREME_DENSE.get())
                        .build(null);
                TYPE_REF.set(type);
                return type;
            });

    public static final RegistryObject<BlockEntityType<EnergizingRodBlockEntity>> ROD_TYPE =
            BLOCK_ENTITIES.register("energizing_rods", () -> {
                Block[] blocks = new Block[14];
                int i = 0;
                for (RodTier tier : RodTier.values()) {
                    blocks[i++] = AE_RODS.get(tier).get();
                }
                for (RodTier tier : RodTier.values()) {
                    blocks[i++] = ME_RODS.get(tier).get();
                }
                BlockEntityType<EnergizingRodBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new EnergizingRodBlockEntity(ROD_TYPE_REF.get(), pos, state), blocks)
                        .build(null);
                ROD_TYPE_REF.set(type);
                return type;
            });

    private static Block[] allRodBlocks() {
        // Called only when building BlockEntityType — registries are available
        Block[] all = new Block[14];
        int i = 0;
        for (var e : AE_RODS.values()) {
            all[i++] = e.get();
        }
        for (var e : ME_RODS.values()) {
            all[i++] = e.get();
        }
        return all;
    }

    private APCells() {
    }

    public static void register(IEventBus bus, boolean registerMeRods) {
        // ME rods always registered as blocks; recipes gated by conditions.
        // If appflux missing, ME blocks still exist but cannot pull FE (logged in tick path).
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(APCells::onCommonSetup);
        AppliedPowah.LOG.info("Registered cells + full-block rods (meRodsRecipeHint={})", registerMeRods);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var cellType = ENERGY_CELLS.get();
            bindCell((appeng.block.AEBaseEntityBlock<?>) SUPER_DENSE.get(), cellType);
            bindCell((appeng.block.AEBaseEntityBlock<?>) EXTREME_DENSE.get(), cellType);

            var rodType = ROD_TYPE.get();
            for (var e : AE_RODS.values()) {
                bindRod((appeng.block.AEBaseEntityBlock<?>) e.get(), rodType);
            }
            for (var e : ME_RODS.values()) {
                bindRod((appeng.block.AEBaseEntityBlock<?>) e.get(), rodType);
            }
            AppliedPowah.LOG.info("Bound energy cells + rod block entities");
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindCell(appeng.block.AEBaseEntityBlock block, BlockEntityType<EnergyCellBlockEntity> type) {
        block.setBlockEntity(EnergyCellBlockEntity.class, type, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindRod(appeng.block.AEBaseEntityBlock block, BlockEntityType<EnergizingRodBlockEntity> type) {
        block.setBlockEntity(EnergizingRodBlockEntity.class, type, null, null);
    }
}
