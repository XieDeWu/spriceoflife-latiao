package com.xdw.spiceoflifelatiao;

import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.event.LevelEventHandle;
import com.xdw.spiceoflifelatiao.event.ManualFoodComponentEvent;
import com.xdw.spiceoflifelatiao.event.PlayerEventHandle;
import com.xdw.spiceoflifelatiao.linkage.FoodItemEvent;
import com.xdw.spiceoflifelatiao.network.SyncHandler;
import com.xdw.spiceoflifelatiao.command.FoodCommand;
import com.xdw.spiceoflifelatiao.config.ManualFoodConfig;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SpiceOfLifeLatiao.MODID)
public class SpiceOfLifeLatiao {
    public static final String MODID = "spiceoflifelatiao";
    public static final String VERSION = "1.3.3";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpiceOfLifeLatiao(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(SyncHandler::onRegisterPayloadHandler);
        modEventBus.addListener(EventPriority.LOWEST, ManualFoodComponentEvent::modifyDefaultComponents);
        modEventBus.addListener(ConfigCached::onConfigLoaded);
        modEventBus.addListener(ConfigCached::onConfigReload);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(LevelEventHandle.class);
        NeoForge.EVENT_BUS.register(PlayerEventHandle.class);
        NeoForge.EVENT_BUS.register(FoodItemEvent.class);
        NeoForge.EVENT_BUS.register(FoodCommand.class);
        ManualFoodConfig.load();
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
