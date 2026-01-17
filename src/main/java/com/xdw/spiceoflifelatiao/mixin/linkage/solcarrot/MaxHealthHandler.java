package com.xdw.spiceoflifelatiao.mixin.linkage.solcarrot;

import com.xdw.spiceoflifelatiao.linkage.solcarrot.FoodTrackerCached;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.cazsius.solcarrot.tracking.MaxHealthHandler")
public class MaxHealthHandler  {
    @Inject(
            method = "updateFoodHPModifier",
            at = @At("HEAD"),
            remap = false,
            cancellable = true)
    private static void updateFoodHPModifierStart(Player player, CallbackInfoReturnable<Boolean> cir) {
        if(FoodTrackerCached.flag && !FoodTrackerCached.hasTriedNewFood.orElse(false)) cir.cancel();
    }
    @Inject(
            method = "updateFoodHPModifier",
            at = @At("TAIL"),
            remap = false,
            cancellable = true)
    private static void updateFoodHPModifierEnd(Player player, CallbackInfoReturnable<Boolean> cir) {
        if(FoodTrackerCached.flag) cir.setReturnValue(cir.getReturnValueZ() && FoodTrackerCached.hasTriedNewFood.orElse(true));
    }
}
