package com.coala.appliedpowah.orb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Auto Energizing Orb block.
 * Same collision and placement as ME/Advanced orb.
 */
public class AutoEnergizingOrbBlock extends EnergizingOrbBlock<AutoEnergizingOrbBlockEntity> {

    public AutoEnergizingOrbBlock() {
        super(Properties.of().strength(2.0F).noOcclusion().dynamicShape());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = APOrbs.autoType();
        return type == null ? null : type.create(pos, state);
    }
}
