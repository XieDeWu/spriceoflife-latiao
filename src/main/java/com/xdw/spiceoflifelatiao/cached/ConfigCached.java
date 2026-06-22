package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.Config;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigCached {
    public static boolean ENABLE_CHANGE = false;
    public static boolean ENABLE_LOSS = false;
    public static boolean ENABLE_HUNGER = false;
    public static boolean ENABLE_SATURATION = false;
    public static boolean ENABLE_EAT_SECONDS = false;
    public static boolean ENABLE_ACTIONS_LOSS = false;
    public static int HISTORY_LENGTH_LONG = 512;
    public static int HISTORY_LENGTH_SHORT = 16;
    public static List<? extends String> LOSS = List.of("0");
    public static List<? extends String> HUNGER = List.of("HUNGER_ORG");
    public static List<? extends String> SATURATION = List.of("SATURATION_ORG");
    public static List<? extends String> EAT_SECONDS = List.of("EAT_SECONDS_ORG");
    public static List<? extends String> BLACK_FOOD = List.of("");
    public static HashMap<Integer,Float> BLACK_FOOD_T = new HashMap<>();
    public static double ACTION_MOVE   = 0D;
    public static double ACTION_JUMP   = 0D;
    public static double ACTION_SWIM   = 0D;
    public static double ACTION_CLIMB  = 0D;
    public static double ACTION_CLICK  = 0D;
    public static double ACTION_USE    = 0D;
    public static double ACTION_FLYING = 0D;
    public static int CONFIG_VERSION_FLAG = -1;

    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            update();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            update();
        }
    }

    private static void update(){
        // 检查配置版本 当前值与默认值不一致时 重置为默认值
        if(!Config.CONFIG_VERSION_FLAG.get().equals(Config.current_config_version)){
            Config.CONFIG_VALUES.forEach(entry -> {
                @SuppressWarnings("unchecked")
                ModConfigSpec.ConfigValue<Object> value = (ModConfigSpec.ConfigValue<Object>) entry;
                value.set(value.getDefault());
            });
            // 对于版本标识符 则另外重置
            Config.CONFIG_VERSION_FLAG.set(Config.current_config_version);
        }

        // 同步服务器配置并缓存
        ENABLE_CHANGE = Config.ENABLE_CHANGE.get();
        ENABLE_LOSS = Config.ENABLE_LOSS.get();
        ENABLE_HUNGER = Config.ENABLE_HUNGER.get();
        ENABLE_SATURATION = Config.ENABLE_SATURATION.get();
        ENABLE_EAT_SECONDS = Config.ENABLE_EAT_SECONDS.get();
        ENABLE_ACTIONS_LOSS = Config.ENABLE_ACTIONS_LOSS.get();
        HISTORY_LENGTH_LONG = Config.HISTORY_LENGTH_LONG.get();
        HISTORY_LENGTH_SHORT = Config.HISTORY_LENGTH_SHORT.get();
        LOSS = Config.LOSS.get();
        HUNGER = Config.HUNGER.get();
        SATURATION = Config.SATURATION.get();
        EAT_SECONDS = Config.EAT_SECONDS.get();
        BLACK_FOOD = Config.BLACK_FOOD.get();
        var ts = new HashMap<Integer,Float>();
        BLACK_FOOD.forEach(i -> {
            String[] split = i.split(",");
            if (split.length == 1) {
                ts.put(split[0].hashCode(), 0F);
            } else if (split.length == 2) {
                ts.put(split[0].hashCode(), Stream.of(split[1])
                        .map(s -> {
                            try {
                                return Float.parseFloat(s);
                            } catch (Exception e) {
                                return 0F;
                            }
                        })
                        .findFirst()
                        .filter(j -> !(j.isNaN() || j.isInfinite()))
                        .map(j -> Math.clamp(j, 0F, 1F))
                        .orElse(0F)
                );
            }
        });
        BLACK_FOOD_T = ts;
        ACTION_MOVE   = Config.ACTION_MOVE  .get();
        ACTION_JUMP   = Config.ACTION_JUMP  .get();
        ACTION_SWIM   = Config.ACTION_SWIM  .get();
        ACTION_CLIMB  = Config.ACTION_CLIMB .get();
        ACTION_CLICK  = Config.ACTION_CLICK .get();
        ACTION_USE    = Config.ACTION_USE   .get();
        ACTION_FLYING = Config.ACTION_FLYING.get();
        CONFIG_VERSION_FLAG = Config.CONFIG_VERSION_FLAG.get();

        EatFormulaContext.clearFormulaCached();
    }
}