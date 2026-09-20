package com.coala.appliedpowah.integration.appflux;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;

/**
 * Applied Flux FE extraction. Only invoked when appflux is loaded.
 */
public final class FluxBridge {
    private FluxBridge() {
    }

    public static long extractFe(IStorageService storage, long amount) {
        if (storage == null || amount <= 0) {
            return 0L;
        }
        return storage.getInventory().extract(
                FluxKey.of(EnergyType.FE),
                amount,
                Actionable.MODULATE,
                IActionSource.empty());
    }
}
