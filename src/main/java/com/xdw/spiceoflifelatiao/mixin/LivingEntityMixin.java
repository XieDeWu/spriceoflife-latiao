package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.event.PlayerEventHandle;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("TAIL"))
    private void onSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) return;
        Optional.ofNullable(PlayerEventHandle.playerSwingRecord.remove(player.getUUID()))
                .ifPresent(it-> PlayerEventHandle.regPlayerAction
                        .apply(player.getUUID())
                        .accept(it.getKey(),it.getValue())
                );
    }
}