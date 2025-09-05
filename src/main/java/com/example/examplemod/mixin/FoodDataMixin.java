package com.example.examplemod.mixin;

import com.example.examplemod.FoodQueueRecord;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements FoodQueueRecord {

}
