package com.coala.appliedpowah.orb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Fluix Energizing Orb block ({@code fluix_energizing_orb}). */
public class MeEnergizingOrbBlock extends EnergizingOrbBlock<MeEnergizingOrbBlockEntity> {
    public MeEnergizingOrbBlock() {
        super(Properties.of().strength(2.5F).noOcclusion());
    }

    @Override
    public MeEnergizingOrbBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = APOrbs.fluixType();
        return type == null ? null : type.create(pos, state);
    }
}
