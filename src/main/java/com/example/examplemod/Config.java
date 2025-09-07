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

    public static final ModConfigSpec.ConfigValue<Boolean> EANBLE_CHANGE = BUILDER
            .comment("启用生活调味料饮食调整，这将根据长短期的食用次数、营养水平、饮食水平、饥饿程度、饱和程度动态影响食物数据，已经过多次平衡性调整以适配农夫乐事前中后期玩家")
            .define("enable_change",true);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_LONG = BUILDER
            .comment("长期食用历史长度,用于统计长期饮食数据")
            .defineInRange("historyLengthLong", 512, 0, 4096);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_SHORT = BUILDER
            .comment("短期食用历史长度,用于统计短期饮食数据")
            .defineInRange("historyLengthShort", 16, 0, 1024);

    public static final ModConfigSpec.ConfigValue<String> LOSS = BUILDER
            .comment("默认自然流失公式，影响顺序：饥饿程度、短期营养水平、短期饮食水平、饱和程度 ||| 相关变量 玩家饱食度 HUNGER_LEVEL 短期总饱和度 SATURATION_SHORT 短期总饱食度 HUNGER_SHORT 玩家饱和度 SATURATION")
            .define("loss", "0.01*Math.pow(2.0,(HUNGER_LEVEL-20.0)/10.0)*Math.pow(0.5,SATURATION_SHORT/Math.max(HUNGER_SHORT,1.0)-1.0)*Math.pow(2.0,(HUNGER_SHORT+SATURATION_SHORT-128.0)/128.0)+0.01*(Math.pow(2.0,(Math.min(SATURATION,40)-16.0)/4.0)-0.0625)");

    public static final ModConfigSpec.ConfigValue<String> HUNGER = BUILDER
        .comment("默认饱食度公式，影响顺序：短期饮食、饥饿程度、长期饮食 ||| 相关变量 现食物饱食度 HUNGER 现食物短期食用次数 EATEN_SHORT 玩家饱食度 HUNGER_LEVEL 现食物长期食用次数 EATEN_LONG")
        .define("hunger", "HUNGER*0.4*Math.max(Math.pow(0.8,EATEN_SHORT),Math.max(1-HUNGER_LEVEL/12.0,0.0))+HUNGER*0.4*Math.max(1-EATEN_LONG/64.0,0.0)+HUNGER*0.2");

    public static final ModConfigSpec.ConfigValue<String> SATURATION = BUILDER
        .comment("默认饱和度公式,影响顺序：短期饮食、长期饮食、饥饿程度 ||| 相关变量 现食物饱和度 SATURATION 现食物短期食用次数 EATEN_SHORT 现食物长期食用次数 EATEN_LONG 玩家饱食度 HUNGER_LEVEL")
        .define("saturation", "SATURATION*Math.pow(0.9,EATEN_SHORT)*Math.max(1.0-EATEN_LONG/64.0,0.0)+(HUNGER*0.2+SATURATION*0.2)*Math.max(1.0-HUNGER_LEVEL/12.0,0.0)");

    public static final ModConfigSpec.ConfigValue<Boolean> EANBLE_ASITIA = BUILDER
            .comment("启用生活调味料厌食调整，这将根据食物的饮食数据调整食用时间")
            .define("enable_asitia",true);

    public static final ModConfigSpec.ConfigValue<String> EAT_SECONDS = BUILDER
            .comment("默认食用时间公式,影响顺序：原食用时间、饥饿程度、原食物饱食度、饮食调整 ||| 相关变量 原食用时间 EAT_SECONDS_ORG 玩家饱食度 HUNGER_LEVEL 原食物饱食度 HUNGER_ORG 现食物饱食度 HUNGER 原食物饱和度 SATURATION_ORG 现食物饱和度 SATURATION")
            .define("EAT_SECONDS","EAT_SECONDS_ORG*(49.0/30.0*Math.pow(10.0/7.0,HUNGER_LEVEL/10.0)−4.0/3.0)");


    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);


    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
