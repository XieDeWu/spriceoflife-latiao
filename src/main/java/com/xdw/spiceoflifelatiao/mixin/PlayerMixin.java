package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.network.SyncHandler;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import com.xdw.spiceoflifelatiao.util.IPlayerAcessor;
import net.minecraft.server.level.ServerPlayer;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerAcessor {
    @Shadow public abstract FoodData getFoodData();
    @Shadow protected abstract float getBlockSpeedFactor();

    @Shadow
    public float oBob;

    @Inject(at = @At(value = "TAIL"), method = "eat")
    public void injected(Level pLevel, ItemStack pFood, FoodProperties pFoodProperties, CallbackInfoReturnable info) {
        var foodData = this.getFoodData();
        if (!(foodData instanceof IEatHistoryAcessor acessor)) return;
        int count = pFood.getCount();
        pFood.setCount(1);
        Optional<EatFormulaContext> from = EatFormulaContext.from((Player) (Object) this, pFood);
        AtomicInteger realHunger = new AtomicInteger(pFoodProperties.nutrition());
        AtomicReference<Float> newRoundErr = new AtomicReference<>(0f);
        from.ifPresent(x->{
            float expectHunger = x.hunger()+x.hungerAccRoundErr();
            int _realHunger = new BigDecimal(expectHunger).setScale(0, RoundingMode.HALF_EVEN).intValue();
            Float _accErr = expectHunger - (float)_realHunger;
            realHunger.set(_realHunger);
            newRoundErr.set(_accErr);

        });
        acessor.addEatHistory(EatHistory.getFoodHash(pFood.getItem()),realHunger.get()*1.0f,pFoodProperties.saturation(),1.0f,newRoundErr.get());
        pFood.setCount(count);
        if ((Object)this instanceof ServerPlayer player){
            SyncHandler.syncEatHistory(player,Optional.empty());
        }
    }

    @Override
    public float getBlockSpeedFactor_public() {
        return getBlockSpeedFactor();
    }
}
