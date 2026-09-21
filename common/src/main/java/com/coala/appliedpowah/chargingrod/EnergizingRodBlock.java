package com.coala.appliedpowah.chargingrod;

import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import appeng.block.AEBaseEntityBlock;
import com.coala.appliedpowah.config.APConfig;
import com.coala.appliedpowah.energycell.APCells;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.phys.shapes.Shapes.join;

/**
 * Full-block AE/ME Energizing Rod. Thin Powah-style collision (not a full cube).
 * Placed only next to AE2 cables; FACING points at the cable.
 */
public class EnergizingRodBlock extends AEBaseEntityBlock<EnergizingRodBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Powah-style thin rod: core at center + shaft toward facing.
        // All box() coords must satisfy min <= max (vanilla Shapes requirement).
        SHAPES.put(Direction.UP, join(box(7, 7, 7, 9, 9, 9),
                join(box(7, 13, 7, 9, 16, 9), box(7.25, 9, 7.25, 8.75, 13, 8.75), BooleanOp.OR), BooleanOp.OR));
        SHAPES.put(Direction.DOWN, join(box(7, 7, 7, 9, 9, 9),
                join(box(7, 0, 7, 9, 3, 9), box(7.25, 3, 7.25, 8.75, 7, 8.75), BooleanOp.OR), BooleanOp.OR));
        SHAPES.put(Direction.NORTH, join(box(7, 7, 7, 9, 9, 9),
                join(box(7, 7, 0, 9, 9, 3), box(7.25, 7.25, 3, 8.75, 8.75, 7), BooleanOp.OR), BooleanOp.OR));
        SHAPES.put(Direction.SOUTH, join(box(7, 7, 7, 9, 9, 9),
                join(box(7, 7, 13, 9, 9, 16), box(7.25, 7.25, 9, 8.75, 8.75, 13), BooleanOp.OR), BooleanOp.OR));
        SHAPES.put(Direction.WEST, join(box(7, 7, 7, 9, 9, 9),
                join(box(0, 7, 7, 3, 9, 9), box(3, 7.25, 7.25, 7, 8.75, 8.75), BooleanOp.OR), BooleanOp.OR));
        SHAPES.put(Direction.EAST, join(box(7, 7, 7, 9, 9, 9),
                join(box(13, 7, 7, 16, 9, 9), box(9, 7.25, 7.25, 13, 8.75, 8.75), BooleanOp.OR), BooleanOp.OR));
    }

    private final RodTier tier;
    private final boolean aeUnit;

    public EnergizingRodBlock(RodTier tier, boolean aeUnit) {
        super(Properties.of().strength(2.0F).noOcclusion().dynamicShape());
        this.tier = tier;
        this.aeUnit = aeUnit;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    public RodTier getTier() {
        return tier;
    }

    public boolean isAeUnit() {
        return aeUnit;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = APCells.ROD_TYPE_REF.get();
        return type == null ? null : type.create(pos, state);
    }

    public static List<Direction> adjacentCableDirs(BlockGetter level, BlockPos pos) {
        List<Direction> dirs = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (isAe2Cable(level, pos.relative(d))) {
                dirs.add(d);
            }
        }
        return dirs;
    }

    public static boolean isAe2Cable(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CableBusBlockEntity;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        List<Direction> cables = adjacentCableDirs(level, pos);
        if (cables.isEmpty()) {
            return null;
        }
        Direction towardClicked = ctx.getClickedFace().getOpposite();
        if (isAe2Cable(level, pos.relative(towardClicked))) {
            return defaultBlockState().setValue(FACING, towardClicked);
        }
        if (cables.size() == 1) {
            return defaultBlockState().setValue(FACING, cables.get(0));
        }
        if (cables.contains(Direction.SOUTH)) {
            return defaultBlockState().setValue(FACING, Direction.SOUTH);
        }
        if (cables.contains(Direction.EAST)) {
            return defaultBlockState().setValue(FACING, Direction.EAST);
        }
        return defaultBlockState().setValue(FACING, cables.get(0));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !adjacentCableDirs(level, pos).isEmpty();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergizingRodBlockEntity rod) {
            rod.scanForOrb();
            rod.onFacingMaybeChanged();
        }
    }

    /** Powah-style drop: keep buffer FE on the item (see AbstractBlock.playerDestroy / storeToStack). */
    @Override
    public void playerDestroy(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                              BlockPos pos, BlockState state, @Nullable BlockEntity te,
                              net.minecraft.world.item.ItemStack tool) {
        if (te instanceof EnergizingRodBlockEntity rod && APConfig.COMMON.rodsKeepEnergyOnBreak.get()) {
            var stack = rod.writeBufferToStack(new net.minecraft.world.item.ItemStack(this));
            popResource(level, pos, stack);
            player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            return;
        }
        super.playerDestroy(level, player, pos, state, te, tool);
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(
            BlockState state, net.minecraft.world.phys.HitResult target,
            BlockGetter level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        var stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof EnergizingRodBlockEntity rod
                && APConfig.COMMON.rodsKeepEnergyOnBreak.get()) {
            return rod.writeBufferToStack(stack);
        }
        return stack;
    }

    @Override
    @Deprecated
    public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        var stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof EnergizingRodBlockEntity rod
                && APConfig.COMMON.rodsKeepEnergyOnBreak.get()) {
            return rod.writeBufferToStack(stack);
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof EnergizingRodBlockEntity rod
                && APConfig.COMMON.rodsKeepEnergyOnBreak.get()) {
            rod.readBufferFromStack(stack);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) {
            return;
        }
        if (!canSurvive(state, level, pos)) {
            // Cable lost: drop with Powah-style buffer when enabled.
            if (APConfig.COMMON.rodsKeepEnergyOnBreak.get()
                    && level.getBlockEntity(pos) instanceof EnergizingRodBlockEntity rod) {
                popResource(level, pos, rod.writeBufferToStack(new net.minecraft.world.item.ItemStack(this)));
            } else {
                dropResources(state, level, pos);
            }
            level.removeBlock(pos, false);
            return;
        }
        Direction facing = state.getValue(FACING);
        if (!isAe2Cable(level, pos.relative(facing))) {
            List<Direction> cables = adjacentCableDirs(level, pos);
            if (!cables.isEmpty()) {
                Direction next = cables.contains(Direction.SOUTH) ? Direction.SOUTH
                        : cables.contains(Direction.EAST) ? Direction.EAST
                        : cables.get(0);
                level.setBlock(pos, state.setValue(FACING, next), 3);
            }
        }
        if (level.getBlockEntity(pos) instanceof EnergizingRodBlockEntity rod) {
            rod.onFacingMaybeChanged();
        }
    }
}
