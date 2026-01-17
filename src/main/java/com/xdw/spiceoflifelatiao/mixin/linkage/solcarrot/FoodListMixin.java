package com.xdw.spiceoflifelatiao.mixin.linkage.solcarrot;

import com.xdw.spiceoflifelatiao.linkage.solcarrot.FoodTrackerCached;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(targets = "com.cazsius.solcarrot.tracking.FoodList")
public class FoodListMixin {
    @Inject(at = @At(value = "TAIL"), method = "addFood")
    public void addFood(ItemStack food, CallbackInfoReturnable<Boolean> cir) {
        if(FoodTrackerCached.flag) FoodTrackerCached.hasTriedNewFood = Optional.ofNullable(cir.getReturnValue());
    }
}
