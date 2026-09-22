package com.coala.appliedpowah.orb;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

import static net.minecraft.world.phys.shapes.Shapes.box;
import static net.minecraft.world.phys.shapes.Shapes.join;

/**
 * AP energizing orb. Default FACING=DOWN (base on the ground, like Powah).
 * ME network connects on DOWN only. Collision = Powah orb (bowl + base plate).
 */
public abstract class EnergizingOrbBlock<T extends MeEnergizingOrbBlockEntity> extends AEBaseEntityBlock<T> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Base plate + bowl (min<=max). DOWN = base at y0, bowl above — matches Powah.
        SHAPES.put(Direction.DOWN, join(
                box(3.5, 5.0, 3.5, 12.5, 14.23, 12.5),
                box(2.5, 0.0, 2.5, 13.5, 1.0, 13.5),
                BooleanOp.OR));
        SHAPES.put(Direction.UP, join(
                box(3.5, 1.77, 3.5, 12.5, 11.0, 12.5),
                box(2.5, 15.0, 2.5, 13.5, 16.0, 13.5),
                BooleanOp.OR));
        SHAPES.put(Direction.NORTH, join(
                box(3.5, 3.5, 0.0, 12.5, 12.5, 1.0),
                box(2.5, 2.5, 5.0, 13.5, 13.5, 14.23),
                BooleanOp.OR));
        SHAPES.put(Direction.SOUTH, join(
                box(3.5, 3.5, 15.0, 12.5, 12.5, 16.0),
                box(2.5, 2.5, 1.77, 13.5, 13.5, 11.0),
                BooleanOp.OR));
        SHAPES.put(Direction.WEST, join(
                box(0.0, 3.5, 3.5, 1.0, 12.5, 12.5),
                box(5.0, 2.5, 2.5, 14.23, 13.5, 13.5),
                BooleanOp.OR));
        SHAPES.put(Direction.EAST, join(
                box(15.0, 3.5, 3.5, 16.0, 12.5, 12.5),
                box(1.77, 2.5, 2.5, 11.0, 13.5, 13.5),
                BooleanOp.OR));
    }

    protected EnergizingOrbBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.DOWN));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        if (com.coala.appliedpowah.config.APConfig.COMMON.orbMultiFacing.get()) {
            state = state.setValue(FACING, ctx.getClickedFace().getOpposite());
        }
        return state;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (isPowahWrenchLink(held)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MeEnergizingOrbBlockEntity orb) {
                // Advanced orb does not accept rod links (it has rod slots + network pull)
                if (orb instanceof AdvancedEnergizingOrbBlockEntity) {
                    player.displayClientMessage(Component.translatable("chat.powah.wrench.link.fail").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.CONSUME;
                }
                CompoundTag nbt = getWrenchNBT(held);
                if (nbt.contains("RodPos", Tag.TAG_COMPOUND)) {
                    BlockPos rodPos = NbtUtils.readBlockPos(nbt.getCompound("RodPos"));
                    BlockEntity rodBe = level.getBlockEntity(rodPos);
                    if (rodBe instanceof com.coala.appliedpowah.chargingrod.EnergizingRodBlockEntity rod) {
                        int range = getPowahRange();
                        if ((int) Math.sqrt(pos.distSqr(rodPos)) <= range) {
                            rod.setOrbPos(pos);
                            player.displayClientMessage(Component.translatable("chat.powah.wrench.link.done").withStyle(ChatFormatting.GOLD), true);
                        } else {
                            player.displayClientMessage(Component.translatable("chat.powah.wrench.link.fail").withStyle(ChatFormatting.RED), true);
                        }
                    }
                    nbt.remove("RodPos");
                } else {
                    nbt.put("OrbPos", NbtUtils.writeBlockPos(pos));
                    player.displayClientMessage(Component.translatable("chat.powah.wrench.link.start").withStyle(ChatFormatting.YELLOW), true);
                }
                return InteractionResult.CONSUME;
            }
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MeEnergizingOrbBlockEntity orb && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable(state.getBlock().getDescriptionId());
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new EnergizingOrbMenu(id, inv, orb);
                }
            }, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static boolean isPowahWrenchLink(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var tag = stack.getTagElement("PowahWrenchNBT");
        return tag != null && tag.getInt("WrenchMode") == 1; // 1 = LINK
    }

    private static CompoundTag getWrenchNBT(ItemStack stack) {
        return stack.getOrCreateTagElement("PowahWrenchNBT");
    }

    private static int getPowahRange() {
        try {
            return owmii.powah.Powah.config().general.energizing_range;
        } catch (Throwable t) {
            return 4;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MeEnergizingOrbBlockEntity orb) {
            dropAll(level, pos, orb.getInv());
            if (orb instanceof AdvancedEnergizingOrbBlockEntity adv) {
                dropAll(level, pos, adv.getRodInv());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void dropAll(Level level, BlockPos pos, net.minecraftforge.items.IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}
