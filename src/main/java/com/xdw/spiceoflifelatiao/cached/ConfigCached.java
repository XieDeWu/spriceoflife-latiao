package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class ConfigCached {
    public static boolean EANBLE_CHANGE = false;
    public static boolean EANBLE_LOSS = false;
    public static boolean EANBLE_HUNGER = false;
    public static boolean EANBLE_SATURATION = false;
    public static boolean EANBLE_EAT_SECONDS = false;
    public static int HISTORY_LENGTH_LONG = 512;
    public static int HISTORY_LENGTH_SHORT = 16;
    public static List<? extends String> LOSS = List.of("0");
    public static List<? extends String> HUNGER = List.of("HUNGER_ORG");
    public static List<? extends String> SATURATION = List.of("SATURATION_ORG");
    public static List<? extends String> EAT_SECONDS = List.of("EAT_SECONDS_ORG");
    public static List<? extends String> BLACK_FOOD = List.of("");
    public static HashMap<Integer,Float> BLACK_FOOD_T = new HashMap<>();

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
        EANBLE_CHANGE = Config.EANBLE_CHANGE.get();
        EANBLE_LOSS = Config.EANBLE_LOSS.get();
        EANBLE_HUNGER = Config.EANBLE_HUNGER.get();
        EANBLE_SATURATION = Config.EANBLE_SATURATION.get();
        EANBLE_EAT_SECONDS = Config.EANBLE_EAT_SECONDS.get();
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
                        .map(j -> Math.max(0F, Math.min(1F, j)))
                        .orElse(0F)
                );
            }
        });
        BLACK_FOOD_T = ts;
    }
}