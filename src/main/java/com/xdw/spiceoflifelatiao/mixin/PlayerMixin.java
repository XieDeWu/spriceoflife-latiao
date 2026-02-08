package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.util.IPlayerAcessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerAcessor {
    @Shadow protected abstract float getBlockSpeedFactor();

    @Override
    public float getBlockSpeedFactor_public() {
        return getBlockSpeedFactor();
    }
}
