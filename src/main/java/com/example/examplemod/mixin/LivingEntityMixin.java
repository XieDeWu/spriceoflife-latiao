package com.example.examplemod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin{
    @Shadow
    public ItemStack eat(Level pLevel, ItemStack pFood, FoodProperties pFoodProperties){return pFood;}

    @Shadow protected abstract float getHealth();
    @Shadow protected abstract float getMaxHealth();
//    @Shadow abstract boolean hasEffect(MobEffect effect);
//    @Shadow public abstract float getMaxHealth();
//    @Shadow public abstract float getHealth();

}