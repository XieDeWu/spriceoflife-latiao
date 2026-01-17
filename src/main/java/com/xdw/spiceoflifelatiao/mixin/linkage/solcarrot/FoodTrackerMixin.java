package com.xdw.spiceoflifelatiao.mixin.linkage.solcarrot;

import com.xdw.spiceoflifelatiao.linkage.solcarrot.FoodTrackerCached;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.cazsius.solcarrot.tracking.FoodTracker")
public class FoodTrackerMixin {
    @Inject(at = @At(value = "HEAD"), method = "onFoodEaten")
    private static void onFoodEatenStart(LivingEntityUseItemEvent.Finish event, CallbackInfo ci){
        FoodTrackerCached.start();
    }
    @Inject(at = @At(value = "TAIL"), method = "onFoodEaten")
    private static void onFoodEatenEnd(LivingEntityUseItemEvent.Finish event, CallbackInfo ci){
        FoodTrackerCached.end();
    }
}
