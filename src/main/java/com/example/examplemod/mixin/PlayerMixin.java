package com.example.examplemod.mixin;

import com.example.examplemod.util.EatHistory;
import com.example.examplemod.util.EatHistoryAcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow public abstract FoodData getFoodData();
    @Inject(at = @At(value = "TAIL"), method = "eat")
    public void injected(Level pLevel, ItemStack pFood, FoodProperties pFoodProperties, CallbackInfoReturnable info) {
        var foodData = this.getFoodData();
        if (!(foodData instanceof EatHistoryAcessor acessor)) return;
        acessor.addEatHistory(EatHistory.getFoodHash(pFood.getItem()),pFoodProperties.nutrition()*1.0f,pFoodProperties.saturation());
    }
}
