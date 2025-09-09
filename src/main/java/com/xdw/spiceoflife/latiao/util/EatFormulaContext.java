package com.xdw.spiceoflife.latiao.util;

import com.xdw.spiceoflife.latiao.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
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

public record EatFormulaContext(
        @NotNull Float hunger_level,
        @NotNull Float saturation_level,
        @NotNull Float sum_hunger_short,
        @NotNull Float sum_hunger_long,
        @NotNull Float sum_saturation_short,
        @NotNull Float sum_saturation_long,
        @NotNull Float loss,
        @NotNull Float hunger_org,
        @NotNull Float saturation_org,
        @NotNull Float eat_seconds_org,
        @NotNull Float buff,
        @NotNull Float debuff,
        @NotNull Float hunger_short,
        @NotNull Float hunger_long,
        @NotNull Float saturation_short,
        @NotNull Float saturation_long,
        @NotNull Float eaten_short,
        @NotNull Float eaten_long,
        @NotNull Float hunger,
        @NotNull Float saturation,
        @NotNull Float eat_seconds
) {

    public static Optional<EatFormulaContext> from(Player player, Optional<ItemStack> item){
        int lengthLong = Config.HISTORY_LENGTH_LONG.get();
        int lengthShort = Math.min(Config.HISTORY_LENGTH_SHORT.get(),lengthLong);
        Optional<Integer> foodHash = item.map(ItemStack::getItem).map(EatHistory::getFoodHash);
        FoodData foodData = player.getFoodData();
        Optional<EatHistory> eatHistory = ((EatHistoryAcessor)foodData)
                .getEatHistory()
                .flatMap(EatHistory::fromBytes);
        Optional<FoodProperties> foodProperties = item.flatMap(x-> Optional.ofNullable(x.get(DataComponents.FOOD)));
        Float hunger_level = (float) foodData.getFoodLevel();
        Float saturation_level = foodData.getSaturationLevel();
        AtomicReference<Float> sum_hunger_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_saturation_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_hunger_short = new AtomicReference<>(0f);
        AtomicReference<Float> sum_saturation_short = new AtomicReference<>(0f);
        Float hunger_org = foodProperties.map(x->(float)x.nutrition()).orElse(0f);
        Float saturation_org = foodProperties.map(FoodProperties::saturation).orElse(0f);
        Float eat_seconds_org = foodProperties.map(FoodProperties::eatSeconds).orElse(0f);
        AtomicReference<Float> buff = new AtomicReference<>(0f);
        AtomicReference<Float> debuff = new AtomicReference<>(0f);
        foodProperties.ifPresent(x->{
            x.effects().forEach(y-> {
                var category = y.effect().getEffect().value().getCategory();
                if(category == MobEffectCategory.BENEFICIAL) buff.updateAndGet(v -> v + 1f);
                if(category == MobEffectCategory.HARMFUL) debuff.updateAndGet(v -> v + 1f);
            });
        });
        AtomicReference<Float> hunger_short = new AtomicReference<>(0f);
        AtomicReference<Float> hunger_long = new AtomicReference<>(0f);
        AtomicReference<Float> saturation_short = new AtomicReference<>(0f);
        AtomicReference<Float> saturation_long = new AtomicReference<>(0f);
        AtomicReference<Float> eaten_short = new AtomicReference<>(0f);
        AtomicReference<Float> eaten_long = new AtomicReference<>(0f);
        record Tuple(int index, int foodHash, float hunger, float saturation) {}
        eatHistory.stream()
                .flatMap(eh -> {
                    List<Integer> foods = eh.foodHash();
                    List<Float> hungers = eh.hunger();
                    List<Float> saturations = eh.saturation();
                    return IntStream.range(0, foods.size()).mapToObj(i -> new Tuple(i, foods.get(i),hungers.get(i),saturations.get(i)));
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
                .filter(tuple -> foodHash.isPresent() && tuple.foodHash == foodHash.get()) // 使用索引过滤
                .forEach(it -> {
                    hunger_long.updateAndGet(v -> v + it.hunger);
                    saturation_long.updateAndGet(v -> v + it.saturation);
                    eaten_long.updateAndGet(v -> v + 1f);
                    if(it.index < lengthShort) {
                        hunger_short.updateAndGet(v -> v + it.hunger);
                        saturation_short.updateAndGet(v -> v + it.saturation);
                        eaten_short.updateAndGet(v -> v + 1f);
                    }
                });
        LinkedHashMap<String, Float> context = new LinkedHashMap<>();
        context.put("HUNGER_LEVEL",hunger_level);
        context.put("SATURATION_LEVEL",saturation_level);
        context.put("SUM_HUNGER_SHORT",sum_hunger_short.get());
        context.put("SUM_HUNGER_LONG", sum_hunger_long.get());
        context.put("SUM_SATURATION_SHORT",sum_saturation_short.get());
        context.put("SUM_SATURATION_LONG", sum_saturation_long.get());
        context.put("HUNGER_ORG",hunger_org);
        context.put("SATURATION_ORG",saturation_org);
        context.put("EAT_SECONDS_ORG",eat_seconds_org);
        context.put("BUFF",buff.get());
        context.put("DEBUFF",debuff.get());
        context.put("HUNGER_SHORT",hunger_short.get());
        context.put("HUNGER_LONG",hunger_long.get());
        context.put("SATURATION_SHORT",saturation_short.get());
        context.put("EATEN_SHORT",eaten_short.get());
        context.put("EATEN_LONG",eaten_long.get());
        try {
            Float loss = (float) eval(Config.LOSS.get(),context).evaluate();
            context.put("LOSS",loss);
            Float hunger = (float) eval(Config.HUNGER.get(),context).evaluate();
            context.put("HUNGER",hunger);
            Float saturation = (float) eval(Config.SATURATION.get(),context).evaluate();
            context.put("SATURATION",saturation);
            Float eat_seconds = (float) eval(Config.EAT_SECONDS.get(),context).evaluate();
            context.put("EAT_SECONDS",eat_seconds);
            return Optional.of(new EatFormulaContext(
                    hunger_level,
                    saturation_level,
                    sum_hunger_short.get(),
                    sum_hunger_long.get(),
                    sum_saturation_short.get(),
                    sum_saturation_long.get(),
                    loss,
                    hunger_org,
                    saturation_org,
                    eat_seconds_org,
                    buff.get(),
                    debuff.get(),
                    hunger_short.get(),
                    hunger_long.get(),
                    saturation_short.get(),
                    saturation_long.get(),
                    eaten_short.get(),
                    eaten_long.get(),
                    hunger,
                    saturation,
                    eat_seconds
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    private static Expression eval(String formula, LinkedHashMap<String,Float> context){
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
        Expression build = exp.build();
        context.keySet().forEach(it->{
            build.setVariable(it,context.get(it));
        });
        return build;
    }
}
