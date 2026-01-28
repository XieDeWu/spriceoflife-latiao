package com.xdw.spiceoflifelatiao.linkage.jade;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.util.CommonProxy;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@WailaPlugin
public class JadePluginAccess implements IWailaPlugin {
    public static final Map<String, Supplier<Supplier<IWailaPlugin>>> PLUGIN_LOADERS = Maps.newHashMap();
    public static IWailaClientRegistration client;
    private final List<IWailaPlugin> plugins = Lists.newArrayList();

    public JadePluginAccess() {
        PLUGIN_LOADERS.forEach((modid, loader) -> {
            if (CommonProxy.isModLoaded(modid)) {
                try {
                    this.plugins.add(loader.get().get());
                } catch (Throwable e) {
//                    LOGGER.error("Failed to load plugin for %s".formatted(modid), e);
                }

            }
        });
    }

    public void register(IWailaCommonRegistration registration) {
        this.plugins.removeIf(($) -> {
            try {
                $.register(registration);
                return false;
            } catch (Throwable e) {
//                JadeAddons.LOGGER.error("Failed to register plugin %s".formatted($.getClass().getName()), e);
                return true;
            }
        });
    }

    public void registerClient(IWailaClientRegistration registration) {
        client = registration;
        this.plugins.forEach(($) -> $.registerClient(registration));
    }

    static {
        PLUGIN_LOADERS.put("spiceoflifelatiao",  ()->FoodInfoPlugin::new);
//        PLUGIN_LOADERS.put("create", (Supplier)() -> CreatePlugin::new);
//        PLUGIN_LOADERS.put("lootr", (Supplier)() -> LootrPlugin::new);
//        PLUGIN_LOADERS.put("enderio", (Supplier)() -> EnderIOPlugin::new);
    }
}