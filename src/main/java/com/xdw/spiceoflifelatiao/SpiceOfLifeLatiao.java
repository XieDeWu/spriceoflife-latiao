package com.xdw.spiceoflifelatiao;

import com.xdw.spiceoflifelatiao.event.PlayerEventHandle;
import com.xdw.spiceoflifelatiao.network.SyncHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SpiceOfLifeLatiao.MODID)
public class SpiceOfLifeLatiao {
    public static final String MODID = "spiceoflifelatiao";
    public static final String VERSION = "1.0.1";

    public SpiceOfLifeLatiao(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(SyncHandler::onRegisterPayloadHandler);
        NeoForge.EVENT_BUS.register(PlayerEventHandle.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
