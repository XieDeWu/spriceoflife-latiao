package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.cached.FoodDataCached;
import com.xdw.spiceoflifelatiao.cached.LevelCalcCached;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


@Mixin(FoodData.class)
public abstract class FoodDataMixin implements IEatHistoryAcessor {
    @Unique private final String eat_history_label = "spiceoflifelatiao:eat_history";
    @Unique private ArrayList<Integer> queueFood = new ArrayList<>();
    @Unique private ArrayList<Float> queueHunger = new ArrayList<>();
    @Unique private ArrayList<Float> queueSaturation = new ArrayList<>();
    @Unique private ArrayList<Float> queueEaten = new ArrayList<>();
    @Unique private float hungerRoundErr = 0;
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Inject(at = @At(value = "TAIL"), method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    public void addAdditionalSaveData(CompoundTag p_38720_, CallbackInfo info) {
        getEatHistory_Bin().ifPresent(x->p_38720_.putByteArray(eat_history_label,x));
    }
    @Inject(at = @At(value = "TAIL"), method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    public void readAdditionalSaveData(CompoundTag p_38716_, CallbackInfo info) {
        if (p_38716_.contains(eat_history_label)) {
            Optional.of(p_38716_.getByteArray(eat_history_label))
                    .ifPresent(this::setEatHistory_Bin);
        }
    }
    @Inject(method = "eat*", at = @At("HEAD"))
    public void eat(FoodProperties foodProperties, CallbackInfo ci) {
        FoodDataCached.foodProperties = Optional.ofNullable(foodProperties);
        FoodDataCached.addHunger = FoodDataCached.foodProperties.map(FoodProperties::nutrition);
        FoodDataCached.addSaturation = FoodDataCached.foodProperties.map(FoodProperties::saturation);
        FoodDataCached.realHunger = FoodDataCached.foodProperties.map(FoodProperties::nutrition);
        FoodDataCached.realSaturation = FoodDataCached.foodProperties.map(FoodProperties::saturation);
    }


    @Inject(method = "add", at = @At("HEAD"),cancellable = true)
    private void add(int foodLevel, float saturationLevel, CallbackInfo ci) {
        ci.cancel();
        AtomicInteger hunger = new AtomicInteger(foodLevel);
        AtomicReference<Float> saturation = new AtomicReference<>(saturationLevel);
        FoodDataCached.addHunger = Optional.of(foodLevel);
        FoodDataCached.addSaturation = Optional.of(saturationLevel);
        if(FoodDataCached.player.isPresent() && FoodDataCached.item.isPresent() && !FoodDataCached.readFoodInfo){
            Optional.of(LevelOrgFoodValue.getBlockFoodInfo(FoodDataCached.player.get(),FoodDataCached.item.get(),null,
                    FoodDataCached.item.map(i->i.get(DataComponents.FOOD)).orElse(null),
                    true, (int) LevelCalcCached.gameTime)).ifPresent(it->{
                var addHunger = it.x / FoodDataCached.bites.orElse(1);
                var addSaturation = it.y / FoodDataCached.bites.orElse(1);
                hunger.set((int) Math.round(addHunger));
                saturation.set((float) addSaturation);
                FoodDataCached.realHunger = Optional.of(hunger.get());
                FoodDataCached.realSaturation = Optional.of(saturation.get());
                FoodDataCached.hungerRoundErr = Optional.of((float) (addHunger - hunger.get()));
            });
        }else {
            FoodDataCached.realHunger = Optional.of(hunger.get());
            FoodDataCached.realSaturation = Optional.of(saturation.get());
        }
        this.foodLevel = Mth.clamp(hunger.get() + this.foodLevel, 0, 20);
        this.saturationLevel = Mth.clamp(saturation.get() + Optional.of(this.saturationLevel).filter(Float::isFinite).orElse(0F), 0.0F, (float)this.foodLevel);
        if(FoodDataCached.flag && FoodDataCached.accessOrderAdd == 0) FoodDataCached.accessOrderAdd = FoodDataCached.numSeq.getAndIncrement();
    }

    @Override
    @Unique public EatHistory getEatHistory_Mem() {
        return new EatHistory(queueFood, queueHunger, queueSaturation, queueEaten, hungerRoundErr);
    }

    @Override
    @Unique public Optional<byte[]> getEatHistory_Bin(){
        return getEatHistory_Mem().toBytes();
    }

    @Override
    @Unique public void setEatHistory_Bin(byte[] eatHistoryBytes){
        EatHistory.fromBytes(eatHistoryBytes)
                .filter(x -> x.foodHash() != null && x.hunger() != null && x.saturation() != null && x.eaten() != null && x.hungerRoundErr() != null )
                .filter(x -> {
                    var length = x.foodHash().size();
                    return x.hunger().size() == length && x.saturation().size() == length && x.eaten().size() == length;
                })
                .ifPresent(eatHistory -> {
                    int length = ConfigCached.HISTORY_LENGTH_LONG;
                    int size = EatFormulaContext.findDynSize(eatHistory.eaten(),length);
                    eatHistory.foodHash().subList(size,eatHistory.foodHash().size()).clear();
                    eatHistory.hunger().subList(size,eatHistory.hunger().size()).clear();
                    eatHistory.saturation().subList(size,eatHistory.saturation().size()).clear();
                    eatHistory.eaten().subList(size,eatHistory.eaten().size()).clear();
                    queueFood = eatHistory.foodHash();
                    queueHunger = eatHistory.hunger();
                    queueHunger.replaceAll(f -> (f != null && Float.isFinite(f)) ? f : 0F);
                    queueSaturation = eatHistory.saturation();
                    queueSaturation.replaceAll(f -> (f != null && Float.isFinite(f)) ? f : 0F);
                    queueEaten = eatHistory.eaten();
                    queueEaten.replaceAll(f -> (f != null && Float.isFinite(f)) ? f : 1F);
                    hungerRoundErr = Optional.of(eatHistory.hungerRoundErr()).filter(i->!i.isNaN() && !i.isInfinite()).orElse(0F);
                });
    }

    @Override
    @Unique public Optional<byte[]> addEatHistory_Mem(Integer foodID, Float hunger, float saturation, float eaten, float hungerRoundErr){
        queueFood.addFirst(foodID);
        queueHunger.addFirst(hunger);
        queueSaturation.addFirst(saturation);
        queueEaten.addFirst(eaten);
        this.hungerRoundErr = hungerRoundErr;

        int length = ConfigCached.HISTORY_LENGTH_LONG;
        int size = EatFormulaContext.findDynSize(queueEaten,length);
        queueFood.subList(size,queueFood.size()).clear();
        queueHunger.subList(size,queueHunger.size()).clear();
        queueSaturation.subList(size,queueSaturation.size()).clear();
        queueEaten.subList(size,queueEaten.size()).clear();
        return Optional.empty();
    }
}
