package com.example.examplemod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {
    @Inject(at = @At(value = "HEAD"), method = "tick(Lnet/minecraft/world/entity/player/Player;)V", cancellable = true)
    private void injected(Player p_38711_, CallbackInfo info){
        int a = 1;
        int b = 2;
    }
}
