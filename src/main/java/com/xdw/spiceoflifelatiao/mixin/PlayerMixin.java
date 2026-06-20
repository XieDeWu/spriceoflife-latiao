package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.event.PlayerEventHandle;
import com.xdw.spiceoflifelatiao.util.IPlayerAcessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerAcessor {
    @Shadow protected abstract float getBlockSpeedFactor();

    @Override
    public float getBlockSpeedFactor_public() {
        return getBlockSpeedFactor();
    }


    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void onJumpFromGround(CallbackInfo ci) {
        Player player = (Player)(Object)this;
        PlayerEventHandle.playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put("jump", Map.entry((float) ConfigCached.ACTION_JUMP, 20));
    }
}
