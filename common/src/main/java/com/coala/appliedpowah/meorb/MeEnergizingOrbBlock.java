package com.coala.appliedpowah.meorb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** ME 充能台 / ME Energizing Orb ({@code me_energizing_orb}). */
public class MeEnergizingOrbBlock extends com.coala.appliedpowah.orb.EnergizingOrbBlock<MeEnergizingOrbBlockEntity> {

    public MeEnergizingOrbBlock() {
        super(Properties.of().strength(2.0F).noOcclusion().dynamicShape());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = com.coala.appliedpowah.orb.APOrbs.meType();
        return type == null ? null : type.create(pos, state);
    }
}
