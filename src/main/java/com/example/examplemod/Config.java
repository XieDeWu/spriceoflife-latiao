package com.example.examplemod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_LONG = BUILDER
            .comment("长期食用历史长度,用于统计长期饮食数据")
            .defineInRange("historyLengthLong", 512, 0, 2048);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_RECENT = BUILDER
            .comment("短期食用历史长度,用于统计短期饮食数据")
            .defineInRange("historyLengthRecent", 16, 0, 2048);

    public static final ModConfigSpec.ConfigValue<String> LOSS = BUILDER
            .comment("默认流失公式,取决于饥饿程度、近期饮食,饮食清淡程度 || 相关变量 ")
            .define("loss", "0.002*Math.pow(???HUNGER_LEVEL+SATURATION_LEVEL,???)*Math.log(???HUNGER_RECENT???)");

    public static final ModConfigSpec.ConfigValue<String> HUNGER = BUILDER
        .comment("默认饱食度公式,取决于近期饮食、长期饮食、饥饿程度 || 相关变量 该食物饱食度 HUNGER 该食物近期食用次数 EATEN_RECENT 玩家饱食度 HUNGER_LEVEL 该食物长期食用次数 EATEN_LONG")
        .define("hunger", "HUNGER*0.4*Math.max(Math.pow(0.7,EATEN_RECENT),Math.max(1-HUNGER_LEVEL/10,0))+HUNGER*0.4*Math.max(1-EATEN_LONG/64,0)+HUNGER*0.2");

    public static final ModConfigSpec.ConfigValue<String> SATURATION = BUILDER
        .comment("默认饱和度公式,取决于近期饮食、长期饮食、饥饿程度 || 相关变量 该食物饱和度 SATURATION 该食物近期食用次数 EATEN_RECENT 该食物长期食用次数 EATEN_LONG 玩家饱食度 HUNGER_LEVEL")
        .define("saturation", "SATURATION*Math.pow(0.7,EATEN_RECENT)*Math.max(1.0-EATEN_LONG/64.0,0.0)+HUNGER*0.3*Math.max(1.0-HUNGER_LEVEL/10.0,0.0)");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);


    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
