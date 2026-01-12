package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class ConfigCached {
    public static boolean EANBLE_CHANGE = false;

    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            EANBLE_CHANGE = Config.EANBLE_CHANGE.get();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            EANBLE_CHANGE = Config.EANBLE_CHANGE.get();
        }
    }
}