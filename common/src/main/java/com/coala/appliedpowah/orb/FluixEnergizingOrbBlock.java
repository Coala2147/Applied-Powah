package com.coala.appliedpowah.orb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 福鲁伊克斯充能台 / Fluix Energizing Orb ({@code fluix_energizing_orb}). */
public class FluixEnergizingOrbBlock extends EnergizingOrbBlock<EnergizingOrbBlockEntity> {

    public FluixEnergizingOrbBlock() {
        super(Properties.of().strength(2.5F).noOcclusion());
    }

    @Override
    public EnergizingOrbBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = APOrbs.fluixType();
        return type == null ? null : type.create(pos, state);
    }
}
