package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.util.EatHistory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(at = @At("HEAD"),method = "getUseDuration")
    public void getUseDurationStart(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        EatHistory.recentEntity = Optional.ofNullable(entity);
    }
}
