package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.cached.FoodDataCached;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(at = @At("HEAD"),method = "finishUsingItem")
    public void finishUsingItemStart(Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
        if(!(livingEntity instanceof Player player)) return;
        FoodDataCached.start(Optional.of(player),Optional.of((ItemStack)(Object)this));
    }
    @Inject(at = @At("TAIL"),method = "finishUsingItem")
    public void finishUsingItemEnd(Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
        FoodDataCached.end();
    }
}
