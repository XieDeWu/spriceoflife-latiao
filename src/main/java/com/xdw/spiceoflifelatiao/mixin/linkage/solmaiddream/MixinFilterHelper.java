package com.xdw.spiceoflifelatiao.mixin.linkage.solmaiddream;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mastermarisa.solmaiddream.utils.FilterHelper")
public class MixinFilterHelper {
    @Inject(
            method = "getInvalidReason",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void fixNull(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<String> cir) {
        if (stack.getFoodProperties(entity) == null) {
            cir.setReturnValue(Component.translatable("jade.solmaiddream.tooltip.blacklist").getString());
        }
    }
}