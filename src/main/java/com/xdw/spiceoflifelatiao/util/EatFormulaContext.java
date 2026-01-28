package com.xdw.spiceoflifelatiao.util;

import com.xdw.spiceoflifelatiao.cached.BlockBehaviourCached;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.cached.EatFormulaCalcCached;
import com.xdw.spiceoflifelatiao.cached.PlayerCalcCached;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record EatFormulaContext(
        @NotNull Float loss,
        @NotNull Float hunger,
        @NotNull Float saturation,
        @NotNull Float eat_seconds,
        @NotNull Float hungerAccRoundErr
) {

    public static Optional<EatFormulaContext> from(Player player, ItemStack item,FoodProperties foodProperties){
        int flag = Stream.of(
                foodProperties != null ? foodProperties.nutrition() : 0,
                Math.round(foodProperties != null ? foodProperties.saturation() : 0),
                BlockBehaviourCached.flag ? 0 : 1
        ).reduce(1, (h, v) -> 31 * h + v);
        return EatFormulaCalcCached.getCached(player,item,flag).or(()->{
            var value = EatFormulaContext.calc(player, item,foodProperties);
            value = configLimit(value,foodProperties);
            EatFormulaCalcCached.addCached(player,item,value,flag);
            return value;
        });
    }
    public static Optional<EatFormulaContext> configLimit(Optional<EatFormulaContext> value,FoodProperties defaultFoodProperties){
        if(value.isEmpty()) return value;
        var v = value.get();
        float loss = ConfigCached.EANBLE_LOSS ? v.loss : 0f;
        float hunger = v.hunger;
        float saturation = v.saturation;
        float eat_seconds = v.eat_seconds;
        if(defaultFoodProperties != null){
            hunger = ConfigCached.EANBLE_HUNGER ? v.hunger : defaultFoodProperties.nutrition();
            saturation = ConfigCached.EANBLE_SATURATION ? v.saturation : defaultFoodProperties.saturation();
            eat_seconds = ConfigCached.EANBLE_EAT_SECONDS ? v.eat_seconds : defaultFoodProperties.eatSeconds();
        }
        return Optional.of(new EatFormulaContext(
                loss,
                hunger,
                saturation,
                eat_seconds,
                v.hungerAccRoundErr
        ));
    }

    public static Optional<EatFormulaContext> calc(Player player, ItemStack item,FoodProperties _foodProperties){

        var _item = Optional.of(item);
        Optional<Integer> foodHash = _item.map(ItemStack::getItem).map(EatHistory::getFoodHash);
        FoodData foodData = player.getFoodData();
        Optional<EatHistory> eatHistory = Optional.ofNullable(((IEatHistoryAcessor) foodData)
                .getEatHistory_Mem());
        int _lengthLong = ConfigCached.HISTORY_LENGTH_LONG;
        int lengthLong = eatHistory.map(x -> findDynSize(x.eaten(), _lengthLong)).orElse(_lengthLong);
        int _lengthShort = ConfigCached.HISTORY_LENGTH_SHORT;
        int __lengthShort = eatHistory.map(x -> findDynSize(x.eaten(), _lengthShort)).orElse(_lengthShort);
        int lengthShort = Math.min(__lengthShort,lengthLong);
//        Optional<FoodProperties> foodProperties = _item.flatMap(x-> Optional.ofNullable(x.get(DataComponents.FOOD)));
        Optional<FoodProperties> foodProperties = Optional.ofNullable(_foodProperties);
        Float hunger_level = (float) foodData.getFoodLevel();
        Float saturation_level = foodData.getSaturationLevel();
        AtomicReference<Float> sum_hunger_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_saturation_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_hunger_short = new AtomicReference<>(0f);
        AtomicReference<Float> sum_saturation_short = new AtomicReference<>(0f);
        Float hunger_org = foodProperties.map(x->(float)x.nutrition()).orElse(0f);
        Float saturation_org = foodProperties.map(FoodProperties::saturation).orElse(0f);
        Float eat_seconds_org = foodProperties.map(FoodProperties::eatSeconds).orElse(0f);
        AtomicReference<Float> food_buff = new AtomicReference<>(0f);
        AtomicReference<Float> food_debuff = new AtomicReference<>(0f);
        foodProperties.ifPresent(x-> x.effects().forEach(y-> {
            switch (y.effect().getEffect().value().getCategory()){
                case BENEFICIAL -> food_buff.updateAndGet(v -> v + 1f);
                case HARMFUL -> food_debuff.updateAndGet(v -> v + 1f);
            }
        }));
        AtomicReference<Float> hunger_short = new AtomicReference<>(0f);
        AtomicReference<Float> hunger_long = new AtomicReference<>(0f);
        AtomicReference<Float> saturation_short = new AtomicReference<>(0f);
        AtomicReference<Float> saturation_long = new AtomicReference<>(0f);
        AtomicReference<Float> eaten_short = new AtomicReference<>(0f);
        AtomicReference<Float> eaten_long = new AtomicReference<>(0f);
        record Tuple(int index, int foodHash, float hunger, float saturation,float eaten) {}
        eatHistory.stream()
                .flatMap(eh -> {
                    List<Integer> foods = eh.foodHash();
                    List<Float> hungers = eh.hunger();
                    List<Float> saturations = eh.saturation();
                    List<Float> eatens = eh.eaten();
                    return IntStream.range(0, foods.size()).mapToObj(i -> new Tuple(i, foods.get(i),hungers.get(i),saturations.get(i),eatens.get(i)));
                })
                .filter(tuple->tuple.index < lengthLong)
                .peek(it->{
                    sum_hunger_long.updateAndGet(v -> v + it.hunger);
                    sum_saturation_long.updateAndGet(v -> v + it.saturation);
                    if(it.index < lengthShort){
                        sum_hunger_short.updateAndGet(v -> v + it.hunger);
                        sum_saturation_short.updateAndGet(v -> v + it.saturation);
                    }
                })
                .filter(tuple -> tuple.foodHash == foodHash.get()) // 使用索引过滤
                .forEach(it -> {
                    hunger_long.updateAndGet(v -> v + it.hunger);
                    saturation_long.updateAndGet(v -> v + it.saturation);
                    eaten_long.updateAndGet(v -> v + it.eaten);
                    if(it.index < lengthShort) {
                        hunger_short.updateAndGet(v -> v + it.hunger);
                        saturation_short.updateAndGet(v -> v + it.saturation);
                        eaten_short.updateAndGet(v -> v + it.eaten);
                    }
                });
        float armor = player.getArmorValue();
        float light = PlayerCalcCached.light;
        float is_wet = player.isInWaterRainOrBubble() ? 1f : 0f;
        float rain_level = PlayerCalcCached.rainLevel;
        float thunder_level = PlayerCalcCached.thunderLevel;
        float block_speed_factor = ((IPlayerAcessor) player).getBlockSpeedFactor_public();
        AtomicReference<Float> player_buff = new AtomicReference<>(0f);
        AtomicReference<Float> player_debuff = new AtomicReference<>(0f);
        player.getActiveEffects().forEach(x->{
            switch (x.getEffect().value().getCategory()){
                case BENEFICIAL -> player_buff.updateAndGet(v -> v + 1);
                case HARMFUL -> player_debuff.updateAndGet(v -> v + 1);
            }
        });
        float player_zzz = player.isSleeping() ? 1f : 0f;
        float player_un_sleeptime = PlayerCalcCached.player_un_sleeptime;

        LinkedHashMap<String, Float> context = new LinkedHashMap<>();
        context.put("HUNGER_LEVEL",hunger_level);
        context.put("SATURATION_LEVEL",saturation_level);
        context.put("SUM_HUNGER_SHORT",sum_hunger_short.get());
        context.put("SUM_HUNGER_LONG", sum_hunger_long.get());
        context.put("SUM_SATURATION_SHORT",sum_saturation_short.get());
        context.put("SUM_SATURATION_LONG", sum_saturation_long.get());
        context.put("ARMOR",armor);
        context.put("LIGHT",light);
        context.put("IS_WET",is_wet);
        context.put("RAIN_LEVEL",rain_level);
        context.put("THUNDER_LEVEL",thunder_level);
        context.put("BLOCK_SPEED_FACTOR",block_speed_factor);
        context.put("PLAYER_BUFF",player_buff.get());
        context.put("PLAYER_DEBUFF",player_debuff.get());
        context.put("PLAYER_ZZZ",player_zzz);
        context.put("PLAYER_UN_SLEEPTIME",player_un_sleeptime);
        context.put("HUNGER_ORG",hunger_org);
        context.put("SATURATION_ORG",saturation_org);
        context.put("EAT_SECONDS_ORG",eat_seconds_org);
        context.put("FOOD_BUFF",food_buff.get());
        context.put("FOOD_DEBUFF",food_debuff.get());
        context.put("HUNGER_SHORT",hunger_short.get());
        context.put("HUNGER_LONG",hunger_long.get());
        context.put("SATURATION_SHORT",saturation_short.get());
        context.put("EATEN_SHORT",eaten_short.get());
        context.put("EATEN_LONG",eaten_long.get());
        try {
            float loss = (float) eval(String.join("",ConfigCached.LOSS),context).evaluate();
            if(Float.isNaN(loss) || Float.isInfinite(loss)) return Optional.empty();
            context.put("LOSS",loss);
            float hunger = (float) eval(String.join("",ConfigCached.HUNGER),context).evaluate();
            if(Float.isNaN(hunger) || Float.isInfinite(hunger)) return Optional.empty();
            context.put("HUNGER",hunger);
            float saturation = (float) eval(String.join("",ConfigCached.SATURATION),context).evaluate();
            if(Float.isNaN(saturation) || Float.isInfinite(saturation)) return Optional.empty();
            context.put("SATURATION",saturation);
            float eat_seconds = (float) eval(String.join("",ConfigCached.EAT_SECONDS),context).evaluate();
            if(Float.isNaN(eat_seconds) || Float.isInfinite(eat_seconds)) return Optional.empty();
            context.put("EAT_SECONDS",eat_seconds);
            return Optional.of(new EatFormulaContext(
                    loss,
                    hunger,
                    saturation,
                    eat_seconds,
                    eatHistory.map(EatHistory::hungerRoundErr).orElse(0f)
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    private static final FifoHashMap<Integer,Expression> expCached = new FifoHashMap<>(8);
    private static Expression eval(String formula, LinkedHashMap<String,Float> context){
        var hashcode = formula.hashCode();
        Expression build;
        if(expCached.get(hashcode) instanceof Expression exp){
            build = exp;
        }else{
            ExpressionBuilder exp = new ExpressionBuilder(formula);
            context.keySet().forEach(exp::variable);
            exp
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
            build = exp.build();
            expCached.put(hashcode,build);
        }
        context.keySet().forEach(it-> build.setVariable(it,context.get(it)));
        return build;
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
