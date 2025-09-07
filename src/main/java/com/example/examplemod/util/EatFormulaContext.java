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
        @NotNull Float eat_seconds
) {

    public static Optional<EatFormulaContext> from(Player player, ItemStack item){
        int foodHash = EatHistory.getFoodHash(item.getItem());
        FoodData foodData = player.getFoodData();
        Optional<EatHistory> eatHistory = ((EatHistoryAcessor)foodData)
                .getEatHistory()
                .flatMap(EatHistory::fromBytes);
        FoodProperties foodProperties = item.get(DataComponents.FOOD);
        if(foodProperties == null) return Optional.empty();
        int lengthLong = Config.HISTORY_LENGTH_LONG.get();
        int lengthShort = Math.min(Config.HISTORY_LENGTH_SHORT.get(),lengthLong);
        Float hunger_level = (float) foodData.getFoodLevel();
        Float saturation_level = foodData.getSaturationLevel();
        Float hunger_org = (float) foodProperties.nutrition();
        Float saturation_org = foodProperties.saturation();
        Float eat_seconds_org = foodProperties.eatSeconds();
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
                .filter(tuple -> tuple.foodHash == foodHash && tuple.index < lengthLong) // 使用索引过滤
                .forEach(tuple -> {
                    if(tuple.index < lengthShort) {
                        hunger_short.updateAndGet(v -> v + tuple.hunger);
                        saturation_short.updateAndGet(v -> v + tuple.saturation);
                        eaten_short.updateAndGet(v -> v + 1f);
                    }
                    hunger_long.updateAndGet(v -> v + tuple.hunger);
                    saturation_long.updateAndGet(v -> v + tuple.saturation);
                    eaten_long.updateAndGet(v -> v + 1f);
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
                    eat_seconds
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
