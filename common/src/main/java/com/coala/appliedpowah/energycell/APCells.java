package com.coala.appliedpowah.energycell;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.chargingrod.EnergizingRodBlock;
import com.coala.appliedpowah.chargingrod.EnergizingRodBlockEntity;
import com.coala.appliedpowah.chargingrod.RodBlockItem;
import com.coala.appliedpowah.chargingrod.RodTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 *
 * Cell BE types follow AE2's pattern: one {@link BlockEntityType} per block,
 * bound to the block at type-creation time, plus
 * {@link AEBaseBlockEntity#registerBlockEntityItem}.
 */
public final class APCells {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPowah.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AppliedPowah.MOD_ID);

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

    /** One BE type per cell block (AE2-style), bound at type creation. */
    public static final RegistryObject<BlockEntityType<?>> SUPER_DENSE_CELL_TYPE =
            BLOCK_ENTITIES.register("super_dense_energy_cell",
                    () -> createCellType(SUPER_DENSE.get(), SUPER_DENSE_ITEM.get()));
    public static final RegistryObject<BlockEntityType<?>> EXTREME_DENSE_CELL_TYPE =
            BLOCK_ENTITIES.register("extreme_dense_energy_cell",
                    () -> createCellType(EXTREME_DENSE.get(), EXTREME_DENSE_ITEM.get()));

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
                // Representative item for AE2 UI (controller / network tool). Rods override
                // getItemFromBlockEntity() per block; this is a type-level fallback.
                AEBaseBlockEntity.registerBlockEntityItem(type, AE_RODS.get(RodTier.STARTER).get().asItem());
                for (Block block : blocks) {
                    bindRod((AEBaseEntityBlock<?>) block, type);
                }
                return type;
            });

    /**
     * AE2-style cell type factory: AtomicReference for the supplier, bind the
     * block immediately, register the representative item for AE2 lookups.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockEntityType<?> createCellType(Block block, Item item) {
        AtomicReference<BlockEntityType<APDenseEnergyCellBlockEntity>> ref = new AtomicReference<>();
        BlockEntityType<APDenseEnergyCellBlockEntity> type = BlockEntityType.Builder
                .of((pos, state) -> new APDenseEnergyCellBlockEntity(ref.get(), pos, state), block)
                .build(null);
        ref.set(type);

        AEBaseBlockEntity.registerBlockEntityItem(type, item);

        AEBaseEntityBlock aeBlock = (AEBaseEntityBlock) block;
        // Server ticker powers APDenseEnergyCellBlockEntity#serverSyncTick (client pick NBT).
        aeBlock.setBlockEntity(EnergyCellBlockEntity.class, (BlockEntityType) type, null,
                (level, pos, state, be) -> {
                    if (be instanceof APDenseEnergyCellBlockEntity cell) {
                        cell.serverSyncTick();
                    }
                });
        AppliedPowah.LOG.info("Bound cell BE type {} → {}", type, block);
        return type;
    }

    private APCells() {
    }

    public static void register(IEventBus bus, boolean registerMeRods) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(APCells::onCommonSetup);
        AppliedPowah.LOG.info("Registered cells + full-block rods (meRodsRecipeHint={})", registerMeRods);
    }

    /** Creative-tab charged cell stacks (AE2 addToMainCreativeTab parity). */
    public static ItemStack chargedCellStack(RegistryObject<Item> item, double maxPower) {
        ItemStack stack = new ItemStack(item.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putDouble("internalCurrentPower", maxPower);
        tag.putDouble("internalMaxPower", maxPower);
        return stack;
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> AppliedPowah.LOG.info(
                "Energy cell BE types bound at registration (super={}, extreme={})",
                SUPER_DENSE_CELL_TYPE.isPresent(), EXTREME_DENSE_CELL_TYPE.isPresent()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindRod(AEBaseEntityBlock block, BlockEntityType<EnergizingRodBlockEntity> type) {
        block.setBlockEntity(EnergizingRodBlockEntity.class, type, null, null);
    }
}
