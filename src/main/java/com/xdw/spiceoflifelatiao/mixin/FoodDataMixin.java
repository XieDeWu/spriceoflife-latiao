package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.Config;
import com.xdw.spiceoflifelatiao.cached.BlockBehaviourCached;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Mixin(FoodData.class)
public abstract class FoodDataMixin implements IEatHistoryAcessor {
    @Unique private final String eat_history_label = "spiceoflifelatiao:eat_history";
    @Unique private LinkedList<Integer> queueFood = new LinkedList<>();
    @Unique private LinkedList<Float> queueHunger = new LinkedList<>();
    @Unique private LinkedList<Float> queueSaturation = new LinkedList<>();
    @Unique private LinkedList<Float> queueEaten = new LinkedList<>();
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Inject(at = @At(value = "TAIL"), method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    public void addAdditionalSaveData(CompoundTag p_38720_, CallbackInfo info) {
        getEatHistory().ifPresent(x->p_38720_.putByteArray(eat_history_label,x));
    }
    @Inject(at = @At(value = "TAIL"), method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    public void readAdditionalSaveData(CompoundTag p_38716_, CallbackInfo info) {
        if (p_38716_.contains(eat_history_label)) {
            Optional.of(p_38716_.getByteArray(eat_history_label))
                    .ifPresent(this::setEatHistory);
        }
    }

    @Inject(method = "add", at = @At("HEAD"),cancellable = true)
    private void add(int foodLevel, float saturationLevel, CallbackInfo ci) {
        ci.cancel();
        AtomicInteger hunger = new AtomicInteger(foodLevel);
        AtomicReference<Float> saturation = new AtomicReference<>(saturationLevel);
        BlockBehaviourCached.foodUpd(foodLevel,saturationLevel);
        BlockBehaviourCached.getContext().ifPresent(x->{
            hunger.set(new BigDecimal(x.hunger()).setScale(0, RoundingMode.HALF_EVEN).intValue());
            saturation.set(x.saturation());
        });
        this.foodLevel = Mth.clamp(hunger.get() + this.foodLevel, 0, 20);
        this.saturationLevel = Mth.clamp(saturation.get() + this.saturationLevel, 0.0F, (float)this.foodLevel);
    }

    @Unique public Optional<byte[]> getEatHistory(){
        return new EatHistory(queueFood, queueHunger, queueSaturation,queueEaten).toBytes();
    }
    @Unique public void setEatHistory(byte[] eatHistoryBytes){
        EatHistory.fromBytes(eatHistoryBytes)
                .filter(x -> x.foodHash() != null && x.hunger() != null && x.saturation() != null && x.eaten() != null )
                .filter(x -> {
                    var length = x.foodHash().size();
                    return x.hunger().size() == length && x.saturation().size() == length && x.eaten().size() == length;
                })
                .ifPresent(eatHistory -> {
                    int length = Config.HISTORY_LENGTH_LONG.get();
                    int size = EatFormulaContext.findSumIndex(eatHistory.eaten(),length).orElse(length);
                    queueFood = eatHistory.foodHash().stream().limit(size).collect(Collectors.toCollection(LinkedList::new));
                    queueHunger = eatHistory.hunger().stream().limit(size).collect(Collectors.toCollection(LinkedList::new));
                    queueSaturation = eatHistory.saturation().stream().limit(size).collect(Collectors.toCollection(LinkedList::new));
                    queueEaten = eatHistory.eaten().stream().limit(size).collect(Collectors.toCollection(LinkedList::new));
                });
    }
    @Unique public Optional<byte[]> addEatHistory(Integer foodID,Float hunger,float saturation,float eaten){
        queueFood.addFirst(foodID);
        queueHunger.addFirst(hunger);
        queueSaturation.addFirst(saturation);
        queueEaten.addFirst(eaten);
        return Optional.empty();
    }
}
