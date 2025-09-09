package com.xdw.spiceoflife.latiao.mixin;

import com.xdw.spiceoflife.latiao.Config;
import com.xdw.spiceoflife.latiao.util.EatHistory;
import com.xdw.spiceoflife.latiao.util.EatHistoryAcessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements EatHistoryAcessor {
    @Unique private final String eat_history_label = "spiceoflifelatiao:eat_history";
    @Unique private LinkedList<Integer> queueFood = new LinkedList<>();
    @Unique private LinkedList<Float> queueHunger = new LinkedList<>();
    @Unique private LinkedList<Float> queueSaturation = new LinkedList<>();

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
    @Unique public Optional<byte[]> getEatHistory(){
        return new EatHistory(queueFood, queueHunger, queueSaturation).toBytes();
    }
    @Unique public void setEatHistory(byte[] eatHistoryBytes){
        EatHistory.fromBytes(eatHistoryBytes)
                .filter(x -> x.foodHash() != null && x.hunger() != null && x.saturation() != null )
                .filter(x -> {
                    var length = x.foodHash().size();
                    return x.hunger().size() == length && x.saturation().size() == length;
                })
                .ifPresent(eatHistory -> {
                    int length = Config.HISTORY_LENGTH_LONG.get();
                    queueFood = eatHistory.foodHash().stream().limit(length).collect(Collectors.toCollection(LinkedList::new));
                    queueHunger = eatHistory.hunger().stream().limit(length).collect(Collectors.toCollection(LinkedList::new));
                    queueSaturation = eatHistory.saturation().stream().limit(length).collect(Collectors.toCollection(LinkedList::new));
                });
    }
    @Unique public Optional<byte[]> addEatHistory(Integer foodID,Float hunger,float saturation){
        queueFood.addFirst(foodID);
        queueHunger.addFirst(hunger);
        queueSaturation.addFirst(saturation);
        return Optional.empty();
    }
}
