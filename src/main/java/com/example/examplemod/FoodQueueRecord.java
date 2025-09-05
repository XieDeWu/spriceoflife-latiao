package com.example.examplemod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public interface FoodQueueRecord {
    void eaten(ItemStack itemStack, FoodProperties foodProperties, Player player);
}
