package com.example.examplemod.util;

import com.example.examplemod.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public record EatFormulaContext(
        @NotNull Float hunger_level,
        @NotNull Float saturation_level,
        @NotNull Float hunger_org,
        @NotNull Float saturation_org,
        @NotNull Float eat_seconds_org,
        @NotNull Float hunger_short,
        @NotNull Float hunger_long,
        @NotNull Float saturation_short,
        @NotNull Float saturation_long,
        @NotNull Float eaten_short,
        @NotNull Float eaten_long,
        @NotNull Float hunger,
        @NotNull Float saturation,
        @NotNull Float eat_seconds,
        @NotNull Float sum_hunger_short,
        @NotNull Float sum_hunger_long,
        @NotNull Float sum_saturation_short,
        @NotNull Float sum_saturation_long,
        @NotNull Float loss
) {

    public static Optional<EatFormulaContext> from(Player player, Optional<ItemStack> item){
        Optional<Integer> foodHash = item.map(ItemStack::getItem).map(EatHistory::getFoodHash);
        FoodData foodData = player.getFoodData();
        Optional<EatHistory> eatHistory = ((EatHistoryAcessor)foodData)
                .getEatHistory()
                .flatMap(EatHistory::fromBytes);
        Optional<FoodProperties> foodProperties = item.map(x->x.get(DataComponents.FOOD));
        if(foodProperties == null) return Optional.empty();
        int lengthLong = Config.HISTORY_LENGTH_LONG.get();
        int lengthShort = Math.min(Config.HISTORY_LENGTH_SHORT.get(),lengthLong);
        Float hunger_level = (float) foodData.getFoodLevel();
        Float saturation_level = foodData.getSaturationLevel();
        Float hunger_org = foodProperties.map(x->(float)x.nutrition()).orElse(0f);
        Float saturation_org = foodProperties.map(FoodProperties::saturation).orElse(0f);
        Float eat_seconds_org = foodProperties.map(FoodProperties::eatSeconds).orElse(0f);
        AtomicReference<Float> hunger_short = new AtomicReference<>(0f);
        AtomicReference<Float> hunger_long = new AtomicReference<>(0f);
        AtomicReference<Float> saturation_short = new AtomicReference<>(0f);
        AtomicReference<Float> saturation_long = new AtomicReference<>(0f);
        AtomicReference<Float> eaten_short = new AtomicReference<>(0f);
        AtomicReference<Float> eaten_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_hunger_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_saturation_long = new AtomicReference<>(0f);
        AtomicReference<Float> sum_hunger_short = new AtomicReference<>(0f);
        AtomicReference<Float> sum_saturation_short = new AtomicReference<>(0f);
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
        try {
            Expression e_hunger = new ExpressionBuilder(Config.HUNGER.get())
                    .variables("HUNGER", "EATEN_SHORT","HUNGER_LEVEL","EATEN_LONG")
                    .function(new Function("max", 2) {
                        @Override
                        public double apply(double... args) {
                            return Math.max(args[0], args[1]);
                        }
                    })
                    .build()
                    .setVariable("HUNGER", hunger_org)
                    .setVariable("EATEN_SHORT", eaten_short.get())
                    .setVariable("HUNGER_LEVEL", hunger_level)
                    .setVariable("EATEN_LONG", eaten_long.get());
            Float hunger = (float) e_hunger.evaluate();
            Expression e_saturation = new ExpressionBuilder(Config.SATURATION.get())
                    .variables("HUNGER","SATURATION","HUNGER_LEVEL", "EATEN_SHORT","EATEN_LONG")
                    .function(new Function("max", 2) {
                        @Override
                        public double apply(double... args) {
                            return Math.max(args[0], args[1]);
                        }
                    })
                    .build()
                    .setVariable("HUNGER", hunger_org)
                    .setVariable("SATURATION",saturation_org)
                    .setVariable("HUNGER_LEVEL", hunger_level)
                    .setVariable("EATEN_SHORT", eaten_short.get())
                    .setVariable("EATEN_LONG", eaten_long.get());
            Float saturation = (float) e_saturation.evaluate();
            Expression e_eat_seconds = new ExpressionBuilder(Config.EAT_SECONDS.get())
                    .variables("HUNGER",
                            "SATURATION",
                            "HUNGER_LEVEL",
                            "EATEN_SHORT",
                            "EATEN_LONG",
                            "EAT_SECONDS_ORG")
                    .function(new Function("max", 2) {
                        @Override
                        public double apply(double... args) {
                            return Math.max(args[0], args[1]);
                        }
                    })
                    .build()
                    .setVariable("HUNGER", hunger_org)
                    .setVariable("SATURATION",saturation_org)
                    .setVariable("HUNGER_LEVEL", hunger_level)
                    .setVariable("EATEN_SHORT", eaten_short.get())
                    .setVariable("EATEN_LONG", eaten_long.get())
                    .setVariable("EAT_SECONDS_ORG",eat_seconds_org);
            Float eat_seconds = (float) e_eat_seconds.evaluate();
            Expression e_loss = new ExpressionBuilder(Config.LOSS.get())
                    .variables("HUNGER_LEVEL",
                            "SATURATION_LEVEL",
                            "SUM_HUNGER_SHORT",
                            "SUM_SATURATION_SHORT")
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
                    })
                    .build()
                    .setVariable("HUNGER_LEVEL", hunger_level)
                    .setVariable("SATURATION_LEVEL", hunger_org)
                    .setVariable("SUM_HUNGER_SHORT",saturation_org)
                    .setVariable("SUM_SATURATION_SHORT",saturation_org);
            Float loss = (float) e_loss.evaluate();
            return Optional.of(new EatFormulaContext(
                    hunger_level,
                    saturation_level,
                    hunger_org,
                    saturation_org,
                    eat_seconds_org,
                    hunger_short.get(),
                    hunger_long.get(),
                    saturation_short.get(),
                    saturation_long.get(),
                    eaten_short.get(),
                    eaten_long.get(),
                    hunger,
                    saturation,
                    eat_seconds,
                    sum_hunger_short.get(),
                    sum_hunger_long.get(),
                    sum_saturation_short.get(),
                    sum_saturation_long.get(),
                    loss
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
