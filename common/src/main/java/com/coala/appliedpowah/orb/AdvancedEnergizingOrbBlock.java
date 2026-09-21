package com.coala.appliedpowah.orb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedEnergizingOrbBlock extends EnergizingOrbBlock<AdvancedEnergizingOrbBlockEntity> {
    public AdvancedEnergizingOrbBlock() {
        super(Properties.of().strength(2.5F).noOcclusion());
    }

    @Override
    public AdvancedEnergizingOrbBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = APOrbs.advType();
        return type == null ? null : type.create(pos, state);
    }
}
