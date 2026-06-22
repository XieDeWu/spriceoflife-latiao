package com.xdw.spiceoflifelatiao.util;

import com.xdw.spiceoflifelatiao.cached.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;

public record EatFormulaContext(
        @NotNull Float loss,
        @NotNull Float hunger,
        @NotNull Float saturation,
        @NotNull Float eat_seconds,
        @NotNull Float hungerAccRoundErr
) {

    public static Optional<EatFormulaContext> from(Player player, ItemStack item, FoodProperties foodProperties, int flag) {
        int _flag = 1;
        _flag = 31 * _flag + (foodProperties != null ? foodProperties.nutrition() : 0);
        _flag = 31 * _flag + Math.round(foodProperties != null ? foodProperties.saturation() : 0);
        _flag = 31 * _flag + (FoodDataCached.flag ? 0 : 1);
        _flag = 31 * _flag + flag;
        var key = EatFormulaCalcCached.makeKey(player,item, _flag);
        return Optional.ofNullable(EatFormulaCalcCached.cachedContext.get(key))
                .or(() -> {
                    var value = EatFormulaContext.calc(player, item, foodProperties);
                    value = configLimit(value, foodProperties);
                    value.map(it->EatFormulaCalcCached.cachedContext.put(key,it));
                    return value;
                });
    }

    public static Optional<EatFormulaContext> configLimit(Optional<EatFormulaContext> value, FoodProperties defaultFoodProperties) {
        if (value.isEmpty()) return value;
        var v = value.get();
        float loss = ConfigCached.ENABLE_LOSS ? v.loss : 0f;
        float hunger = v.hunger;
        float saturation = v.saturation;
        float eat_seconds = v.eat_seconds;
        if (defaultFoodProperties != null) {
            hunger = ConfigCached.ENABLE_HUNGER ? v.hunger : defaultFoodProperties.nutrition();
            saturation = ConfigCached.ENABLE_SATURATION ? v.saturation : defaultFoodProperties.saturation();
            eat_seconds = ConfigCached.ENABLE_EAT_SECONDS ? v.eat_seconds : defaultFoodProperties.eatSeconds();
        }
        return Optional.of(new EatFormulaContext(
                loss,
                hunger,
                saturation,
                eat_seconds,
                v.hungerAccRoundErr
        ));
    }

    /**
     * 通用采样缓存：playerId 为键，interval 为采样间隔（tick）
     */
    private static final Map<Integer, double[]> armorCache = new FifoHashMap<>(16);
    private static final Map<Integer, double[]> wetCache   = new FifoHashMap<>(16);
    private static final Map<Integer, double[]> speedCache = new FifoHashMap<>(16);
    private static double sampleCached(Map<Integer, double[]> cache, int playerId, DoubleSupplier supplier) {
        double[] entry = cache.get(playerId);
        if (entry != null && Math.abs(LevelCalcCached.gameTime - (long) entry[1]) < 20L) {
            return entry[0];
        }
        double value = supplier.getAsDouble();
        cache.put(playerId, new double[]{value, (double) LevelCalcCached.gameTime});
        return value;
    }

    public static Optional<EatFormulaContext> calc(Player player, ItemStack item, FoodProperties _foodProperties) {
        var _item = Optional.of(item);
        Optional<Integer> foodHash = _item.map(ItemStack::getItem).map(EatHistory::getFoodHash);
        FoodData foodData = player.getFoodData();
        Optional<EatHistory> eatHistory = Optional.ofNullable(((IEatHistoryAcessor) foodData).getEatHistory_Mem());

        // ★ 合并 findDynSize：一次遍历同时求出 long 和 short 的边界长度
        final float targetLong = ConfigCached.HISTORY_LENGTH_LONG;
        final float targetShort = ConfigCached.HISTORY_LENGTH_SHORT;
        int lengthLong;
        int lengthShort;
        if (eatHistory.isPresent()) {
            List<Float> eatenList = eatHistory.get().eaten();
            float acc = 0f;
            int size = 0;
            boolean foundLong = false, foundShort = false;
            int longSize = eatenList.size();
            int shortSize = eatenList.size();
            for (float value : eatenList) {
                size++;
                acc += value;
                if (!foundLong && acc >= targetLong) {
                    longSize = size;
                    foundLong = true;
                }
                if (!foundShort && acc >= targetShort) {
                    shortSize = size;
                    foundShort = true;
                }
                if (foundLong && foundShort) break;
            }
            lengthLong = longSize;
            lengthShort = Math.min(shortSize, lengthLong);
        } else {
            lengthLong = (int) targetLong;
            lengthShort = Math.min((int) targetShort, lengthLong);
        }

        Optional<FoodProperties> foodProperties = Optional.ofNullable(_foodProperties);
        float hunger_level = (float) foodData.getFoodLevel();
        float saturation_level = Optional.of(foodData.getSaturationLevel()).filter(Float::isFinite).orElse(0f);

        // 累加变量，完全替代 AtomicReference
        float sum_hunger_long = 0f, sum_saturation_long = 0f;
        float sum_hunger_short = 0f, sum_saturation_short = 0f;
        float hunger_org = foodProperties.map(x -> (float) x.nutrition()).orElse(0f);
        float saturation_org = foodProperties.map(FoodProperties::saturation).orElse(0f);
        float eat_seconds_org = foodProperties.map(FoodProperties::eatSeconds).orElse(0f);

        float food_buff = 0f, food_debuff = 0f;
        if (foodProperties.isPresent()) {
            for (var effect : foodProperties.get().effects()) {
                switch (effect.effect().getEffect().value().getCategory()) {
                    case BENEFICIAL -> food_buff += 1f;
                    case HARMFUL -> food_debuff += 1f;
                }
            }
        }

        float hunger_short = 0f, hunger_long = 0f;
        float saturation_short = 0f, saturation_long = 0f;
        float eaten_short = 0f, eaten_long = 0f;

        final int foodHashVal = foodHash.orElse(0);
        EatHistory history = eatHistory.orElse(null);
        if (history != null) {
            List<Integer> foods = history.foodHash();
            List<Float> hungers = history.hunger();
            List<Float> saturations = history.saturation();
            List<Float> eatens = history.eaten();
            int size = foods.size();
            for (int i = 0; i < size && i < lengthLong; i++) {
                float h = hungers.get(i);
                float s = saturations.get(i);
                // 无条件累加 sum（对应原 peek）
                sum_hunger_long += h;
                sum_saturation_long += s;
                if (i < lengthShort) {
                    sum_hunger_short += h;
                    sum_saturation_short += s;
                }
                // 食物匹配时累加专用变量（对应原 filter + forEach）
                if (foods.get(i) == foodHashVal) {
                    hunger_long += h;
                    saturation_long += s;
                    eaten_long += eatens.get(i);
                    if (i < lengthShort) {
                        hunger_short += h;
                        saturation_short += s;
                        eaten_short += eatens.get(i);
                    }
                }
            }
        }

        int uid = player.getId();

        // ★ 统一采样
        double armor = sampleCached(armorCache, uid, () -> (double) player.getArmorValue());
        double is_wet = sampleCached(wetCache, uid, () -> player.isInWaterRainOrBubble() ? 1.0 : 0.0);
        double block_speed_factor = sampleCached(speedCache, uid, () -> (double) ((IPlayerAcessor) player).getBlockSpeedFactor_public());
        float light = PlayerCalcCached.light;
        float rain_level = PlayerCalcCached.rainLevel;
        float thunder_level = PlayerCalcCached.thunderLevel;

        float player_buff = 0f, player_debuff = 0f;
        for (var effect : player.getActiveEffects()) {
            switch (effect.getEffect().value().getCategory()) {
                case BENEFICIAL -> player_buff += 1;
                case HARMFUL -> player_debuff += 1;
            }
        }
        float player_zzz = player.isSleeping() ? 1f : 0f;
        float player_un_sleeptime = PlayerCalcCached.player_un_sleeptime;

        // 构建 context，完整包含所有原始变量 + 补充 SATURATION_LONG
        Map<String, Double> context = new Object2ObjectOpenHashMap<>(32);
        context.put("HUNGER_LEVEL", (double) hunger_level);
        context.put("SATURATION_LEVEL", (double) saturation_level);
        context.put("SUM_HUNGER_SHORT", (double) sum_hunger_short);
        context.put("SUM_HUNGER_LONG", (double) sum_hunger_long);
        context.put("SUM_SATURATION_SHORT", (double) sum_saturation_short);
        context.put("SUM_SATURATION_LONG", (double) sum_saturation_long);
        context.put("ARMOR", (double) armor);
        context.put("LIGHT", (double) light);
        context.put("IS_WET", (double) is_wet);
        context.put("RAIN_LEVEL", (double) rain_level);
        context.put("THUNDER_LEVEL", (double) thunder_level);
        context.put("BLOCK_SPEED_FACTOR", (double) block_speed_factor);
        context.put("PLAYER_BUFF", (double) player_buff);
        context.put("PLAYER_DEBUFF", (double) player_debuff);
        context.put("PLAYER_ZZZ", (double) player_zzz);
        context.put("PLAYER_UN_SLEEPTIME", (double) player_un_sleeptime);
        context.put("HUNGER_ORG", (double) hunger_org);
        context.put("SATURATION_ORG", (double) saturation_org);
        context.put("EAT_SECONDS_ORG", (double) eat_seconds_org);
        context.put("FOOD_BUFF", (double) food_buff);
        context.put("FOOD_DEBUFF", (double) food_debuff);
        context.put("HUNGER_SHORT", (double) hunger_short);
        context.put("HUNGER_LONG", (double) hunger_long);
        context.put("SATURATION_SHORT", (double) saturation_short);
        context.put("SATURATION_LONG", (double) saturation_long);
        context.put("EATEN_SHORT", (double) eaten_short);
        context.put("EATEN_LONG", (double) eaten_long);

        try {
            double loss = eval(getLossFormula(),context);
            if (Double.isNaN(loss) || Double.isInfinite(loss)) return Optional.empty();
            context.put("LOSS", loss);

            double hunger = eval(getHungerFormula(),context);
            if (Double.isNaN(hunger) || Double.isInfinite(hunger)) return Optional.empty();
            context.put("HUNGER", hunger);

            double saturation = eval(getSaturationFormula(),context);
            if (Double.isNaN(saturation) || Double.isInfinite(saturation)) return Optional.empty();
            context.put("SATURATION", saturation);

            double eat_seconds = eval(getEatSecondsFormula(),context);
            if (Double.isNaN(eat_seconds) || Double.isInfinite(eat_seconds)) return Optional.empty();
            context.put("EAT_SECONDS", eat_seconds);

            float hungerF = (float) hunger;
            float saturationF = (float) saturation;
            float eatSecondsF = (float) eat_seconds;
            float t = ConfigCached.BLACK_FOOD_T.getOrDefault(foodHashVal,1F);
            hungerF = hunger_org + (hungerF - hunger_org) * t;
            saturationF = saturation_org + (saturationF - saturation_org) * t;
            eatSecondsF = eat_seconds_org + (eatSecondsF - eat_seconds_org) * t;

            return Optional.of(new EatFormulaContext(
                    (float) loss, hungerF, saturationF, eatSecondsF,
                    eatHistory.map(EatHistory::hungerRoundErr).orElse(0f)
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    // ========== 公式缓存与求值优化 ==========
    private static final Field EXP_VARIABLES_FIELD;
    static {
        try {
            EXP_VARIABLES_FIELD = Expression.class.getDeclaredField("variables");
            EXP_VARIABLES_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("exp4j internal field 'variables' not found", e);
        }
    }

    private static final Map<String, Expression> EXP_CACHE = new HashMap<>(8);
    private static String lossFormula;
    private static String hungerFormula;
    private static String saturationFormula;
    private static String eatSecondsFormula;

    private static String getLossFormula() {
        if (lossFormula == null) lossFormula = String.join("", ConfigCached.LOSS);
        return lossFormula;
    }
    private static String getHungerFormula() {
        if (hungerFormula == null) hungerFormula = String.join("", ConfigCached.HUNGER);
        return hungerFormula;
    }
    private static String getSaturationFormula() {
        if (saturationFormula == null) saturationFormula = String.join("", ConfigCached.SATURATION);
        return saturationFormula;
    }
    private static String getEatSecondsFormula() {
        if (eatSecondsFormula == null) eatSecondsFormula = String.join("", ConfigCached.EAT_SECONDS);
        return eatSecondsFormula;
    }

    public static void clearFormulaCached() {
        lossFormula = null;
        hungerFormula = null;
        saturationFormula = null;
        eatSecondsFormula = null;
        EXP_CACHE.clear();
    }

    // eval 增加 Map 参数，直接通过反射 set field，不执行 putAll
    private static double eval(String formula, Map<String, Double> context) {
        Expression exp = EXP_CACHE.get(formula);
        if (exp == null) {
            ExpressionBuilder builder = new ExpressionBuilder(formula);
            context.keySet().forEach(builder::variable);
            builder
                    .function(new Function("max", 2) {
                        @Override
                        public double apply(double... args) {
                            return Math.max(args[0], args[1]);
                        }
                    })
                    .function(new Function("min", 2) {
                        @Override
                        public double apply(double... args) {
                            return Math.min(args[0], args[1]);
                        }
                    });
            exp = builder.build();
            EXP_CACHE.put(formula, exp);
        }
        try {
            // 直接将传入的 context 替换到 Expression 内部，O(1) 完成变量设置
            EXP_VARIABLES_FIELD.set(exp, context);
        } catch (IllegalAccessException e) {
            // 理论上不会发生，兜底
            exp.setVariables(context);
        }
        return exp.evaluate();
    }

    public static int findDynSize(List<Float> arr, float target) {
        int size = 0;
        float reduce = 0;
        for (Float value : arr) {
            size++;
            reduce += value;
            if (reduce >= target) {
                return size;
            }
        }
        return size;
    }
}