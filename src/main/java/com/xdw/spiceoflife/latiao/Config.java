package com.xdw.spiceoflife.latiao;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Boolean> EANBLE_CHANGE = BUILDER
            .comment("启用生活调味料饮食调整，这将根据长短期的食用次数、营养水平、饮食水平、饥饿程度、饱和程度动态影响食物数据，适配农夫乐事前中后期玩家")
            .define("enable_change",true);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_LONG = BUILDER
            .comment("长期食用历史长度,用于统计长期饮食数据")
            .defineInRange("historyLengthLong", 512, 0, 4096);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_SHORT = BUILDER
            .comment("短期食用历史长度,用于统计短期饮食数据")
            .defineInRange("historyLengthShort", 16, 0, 1024);

    public static final ModConfigSpec.ConfigValue<String> LOSS = BUILDER
            .comment("默认自然饥饿公式：固定系数、饥饿程度、短期营养、短期摄入、过饱和惩罚")
            .define("LOSS", "0.005*(2^(HUNGER_LEVEL/10-1))" +
                    "*(0.5^(SUM_SATURATION_SHORT/max(SUM_HUNGER_SHORT,1)-1))" +
                    "*(2^((SUM_SATURATION_SHORT+SUM_HUNGER_SHORT)/128 -1))" +
                    "*(1+2^(SATURATION_LEVEL/4-4)-0.0625)");

    public static final ModConfigSpec.ConfigValue<String> HUNGER = BUILDER
        .comment("默认饱食度公式：短期饮食、饥饿程度、长期饮食、最低点")
        .define("HUNGER", "HUNGER_ORG*0.4*max((0.8^EATEN_SHORT),max(1-HUNGER_LEVEL/12,0))" +
                "+HUNGER_ORG*0.4*max(1-EATEN_LONG/64,0)" +
                "+HUNGER_ORG*0.2");

    public static final ModConfigSpec.ConfigValue<String> SATURATION = BUILDER
        .comment("默认饱和度公式：短期饮食、长期饮食、饥饿程度")
        .define("SATURATION", "SATURATION_ORG" +
                "*(0.9^EATEN_SHORT)" +
                "*max(1-EATEN_LONG/64,0)" +
                "+(HUNGER_ORG*0.2+SATURATION_ORG*0.2)" +
                "*max(1-HUNGER_LEVEL/12,0)");

    public static final ModConfigSpec.ConfigValue<String> EAT_SECONDS = BUILDER
            .comment("默认食用时间公式：食物原食用时间、食物原饱食度与饱食偏移、饥饿程度、食物效果数")
            .define("EAT_SECONDS","EAT_SECONDS_ORG" +
                    "*(0.5+0.1*(2*HUNGER_ORG-HUNGER))" +
                    "*(49/30*(10/7)^(HUNGER_LEVEL/10)-4/3)" +
                    "*(1/(1+BUFF))*(2-1/(1+DEBUFF))");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);


    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
