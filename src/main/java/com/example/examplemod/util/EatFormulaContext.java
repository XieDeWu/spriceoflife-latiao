package com.example.examplemod.util;

import com.example.examplemod.Config;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;

import java.util.Optional;

public record EatFormulaContext(
        Config config,
        EatHistory EatHistory,
        Player player,
        FoodProperties foodProperties,
        Float hunger_level,
        Float saturation_level,
        Float hunger_org,
        Float saturation_org,
        Float eat_seconds_org,
        Float hunger_short,
        Float hunger_long,
        Float saturation_short,
        Float saturation_long,
        Float eaten_short,
        Float eaten_long,
        Float hunger,
        Float saturation,
        Float eat_seconds
) {
    public static Optional<EatFormulaContext> From(Config config, Player player, FoodProperties foodProperties){

        return Optional.empty();
    }
}
