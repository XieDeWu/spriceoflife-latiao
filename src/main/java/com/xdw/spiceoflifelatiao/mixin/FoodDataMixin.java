package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.cached.BlockBehaviourCached;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodData;
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
import java.util.stream.Collectors;


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

    @Inject(method = "add", at = @At("HEAD"),cancellable = true)
    private void add(int foodLevel, float saturationLevel, CallbackInfo ci) {
        ci.cancel();
        AtomicInteger hunger = new AtomicInteger(foodLevel);
        AtomicReference<Float> saturation = new AtomicReference<>(saturationLevel);
        BlockBehaviourCached.foodUpd(foodLevel,saturationLevel);
        BlockBehaviourCached.getContext().ifPresent(x->{
            var expectHunger = x.hunger()+x.hungerAccRoundErr();
            hunger.set(Math.round(expectHunger));
            saturation.set(x.saturation());
            BlockBehaviourCached.realHunger = Optional.of(hunger.get());
            BlockBehaviourCached.realSaturation = Optional.of(x.saturation());
            BlockBehaviourCached.hungerRoundErr = Optional.of(expectHunger - hunger.get());
        });
        this.foodLevel = Mth.clamp(hunger.get() + this.foodLevel, 0, 20);
        this.saturationLevel = Mth.clamp(saturation.get() + this.saturationLevel, 0.0F, (float)this.foodLevel);
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
                    int size = EatFormulaContext.findSumIndex(eatHistory.eaten(),length).orElse(length);
                    queueFood = eatHistory.foodHash().stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
                    queueHunger = eatHistory.hunger().stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
                    queueSaturation = eatHistory.saturation().stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
                    queueEaten = eatHistory.eaten().stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
                    hungerRoundErr = eatHistory.hungerRoundErr();
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
        int size = EatFormulaContext.findSumIndex(queueEaten,length).orElse(length);
        queueFood = queueFood.stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
        queueHunger = queueHunger.stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
        queueSaturation = queueSaturation.stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
        queueEaten = queueEaten.stream().limit(size).collect(Collectors.toCollection(ArrayList::new));
        return Optional.empty();
    }
}
