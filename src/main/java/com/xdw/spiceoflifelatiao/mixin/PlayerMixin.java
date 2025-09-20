package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import com.xdw.spiceoflifelatiao.util.IPlayerAcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerAcessor {
    @Shadow public abstract FoodData getFoodData();
    @Shadow protected abstract float getBlockSpeedFactor();
    @Inject(at = @At(value = "TAIL"), method = "eat")
    public void injected(Level pLevel, ItemStack pFood, FoodProperties pFoodProperties, CallbackInfoReturnable info) {
        var foodData = this.getFoodData();
        if (!(foodData instanceof IEatHistoryAcessor acessor)) return;
        acessor.addEatHistory(EatHistory.getFoodHash(pFood.getItem()),pFoodProperties.nutrition()*1.0f,pFoodProperties.saturation(),1.0f);
    }

    @Override
    public float getBlockSpeedFactor_public() {
        return getBlockSpeedFactor();
    }
}
