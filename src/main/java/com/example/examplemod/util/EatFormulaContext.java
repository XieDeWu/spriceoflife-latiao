package com.example.examplemod.util;

import com.example.examplemod.Config;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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
    public static Optional<EatFormulaContext> from(ModConfigSpec config, Player player, Item item){
        int foodHash = EatHistory.getFoodHash(item);
        Optional<EatHistory> eatHistory = ((EatHistoryAcessor) player.getFoodData())
                .getEatHistory()
                .flatMap(EatHistory::fromBytes);
        return Optional.empty();
    }
}
