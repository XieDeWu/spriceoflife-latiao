package com.xdw.spiceoflifelatiao.linkage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface IFoodItem {
    void initState();
    Optional<FoodProperties> getFoodProperties(ItemStack stack, LivingEntity entity);
}
