package com.example.examplemod.mixin;

import com.example.examplemod.FoodQueueRecord;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements FoodQueueRecord {
    @Shadow private int lastFoodLevel;
    @Shadow private int foodLevel;
    @Shadow private float exhaustionLevel;
    @Shadow private float saturationLevel;
    @Shadow private int tickTimer;

    @Shadow public abstract void eat(int p_38708_, float p_38709_);

    public void eaten(ItemStack itemStack, FoodProperties properties, Player player) {
        this.saturationLevel = Math.min(this.saturationLevel + properties.saturation(), 20.0F);
        this.eat(properties.nutrition(), 0.0F);
    }
    @Inject(at = @At(value = "HEAD"), method = "tick(Lnet/minecraft/world/entity/player/Player;)V", cancellable = true)
    private void injected(Player p_38711_, CallbackInfo info){
        int a = 1;
        int b = 2;
    }


}
