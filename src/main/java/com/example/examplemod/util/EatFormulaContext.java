package com.example.examplemod.util;

import com.example.examplemod.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public record EatFormulaContext(
//        Config config,
//        Player player,
//        Item item,
//        EatHistory EatHistory,
//        FoodProperties foodProperties,
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
        Expression e = new ExpressionBuilder("3 * sin(y) - 2 / (x - 2)")
                .variables("x", "y")
                .build()
                .setVariable("x", 2.3)
                .setVariable("y", 3.14);
        double result = e.evaluate();
//        Float hunger
//        Float saturation
//        Float eat_seconds
        return Optional.empty();
    }
}
