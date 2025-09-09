package com.xdw.spiceoflifelatiao;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Boolean> EANBLE_CHANGE = BUILDER
            .comment("公式上下文与计算顺序如下，仅支持向前引用")
            .comment("默认数值均为0.0，计算异常默认使用原版方法运行")
            .comment("HUNGER_LEVEL 玩家饱食度")
            .comment("SATURATION_LEVEL 玩家饱和度")
            .comment("SUM_HUNGER_SHORT 玩家饱食短期累计")
            .comment("SUM_HUNGER_LONG 玩家饱食长期累计")
            .comment("SUM_SATURATION_SHORT 玩家饱和短期累计")
            .comment("SUM_SATURATION_LONG 玩家饱和长期累计")
            .comment("LOSS 自然饥饿")
            .comment("HUNGER_ORG 食物原饱食度")
            .comment("SATURATION_ORG 食物原饱和度")
            .comment("EAT_SECONDS_ORG 食物原食用时间")
            .comment("BUFF 食物可能增益数")
            .comment("DEBUFF 食物可能减益数")
            .comment("HUNGER_SHORT 食物饱食短期累计")
            .comment("HUNGER_LONG 食物饱食长期累计")
            .comment("SATURATION_SHORT 食物饱和短期累计")
            .comment("SATURATION_LONG 食物饱和长期累计")
            .comment("EATEN_SHORT 食物短期食用数")
            .comment("EATEN_LONG 食物长期食用数")
            .comment("HUNGER 食物饱食度")
            .comment("SATURATION 食物饱和度")
            .comment("EAT_SECONDS 食物食用时间")
            .define("enable_change",true);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_LONG = BUILDER
            .comment("用于统计长期饮食数据")
            .defineInRange("historyLengthLong", 512, 0, 4096);

    public static final ModConfigSpec.ConfigValue<Integer> HISTORY_LENGTH_SHORT = BUILDER
            .comment("用于统计短期饮食数据")
            .defineInRange("historyLengthShort", 16, 0, 1024);

    public static final ModConfigSpec.ConfigValue<String> LOSS = BUILDER
            .comment("默认：固定系数、饥饿程度、短期营养、短期摄入、过饱和惩罚")
            .define("LOSS", "0.005*(2^(HUNGER_LEVEL/10-1))" +
                    "*(0.5^(SUM_SATURATION_SHORT/max(SUM_HUNGER_SHORT,1)-1))" +
                    "*(2^((SUM_SATURATION_SHORT+SUM_HUNGER_SHORT)/128 -1))" +
                    "*(1+2^(SATURATION_LEVEL/4-4)-0.0625)");

    public static final ModConfigSpec.ConfigValue<String> HUNGER = BUILDER
        .comment("默认：短期饮食、饥饿程度、长期饮食、最低点")
        .define("HUNGER", "HUNGER_ORG*0.4*max((0.8^EATEN_SHORT),max(1-HUNGER_LEVEL/12,0))" +
                "+HUNGER_ORG*0.4*max(1-EATEN_LONG/64,0)" +
                "+HUNGER_ORG*0.2");

    public static final ModConfigSpec.ConfigValue<String> SATURATION = BUILDER
        .comment("默认：短期饮食、长期饮食、饥饿程度")
        .define("SATURATION", "SATURATION_ORG" +
                "*(0.9^EATEN_SHORT)" +
                "*max(1-EATEN_LONG/64,0)" +
                "+(HUNGER_ORG*0.2+SATURATION_ORG*0.2)" +
                "*max(1-HUNGER_LEVEL/12,0)");

    public static final ModConfigSpec.ConfigValue<String> EAT_SECONDS = BUILDER
            .comment("默认：食物原食用时间、食物原饱食度与饱食偏移、饥饿程度、食物效果数")
            .define("EAT_SECONDS","EAT_SECONDS_ORG" +
                    "*(0.5+0.1*(2*HUNGER_ORG-HUNGER))" +
                    "*(49/30*(10/7)^(HUNGER_LEVEL/10)-4/3)" +
                    "*(1/(1+BUFF))*(2-1/(1+DEBUFF))");

    static final ModConfigSpec SPEC = BUILDER.build();

}
