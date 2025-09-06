package com.example.examplemod.mixin;

import com.example.examplemod.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.example.examplemod.Util.*;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Unique private final String eat_history_label = "examplemod:eat_history";
    @Unique private LinkedList<String> queueFood = new LinkedList<>(List.of("apple"));
    @Unique private LinkedList<Float> queueHunger = new LinkedList<>(List.of(22f));
    @Unique private LinkedList<Float> queueSaturation = new LinkedList<>(List.of(33f));

    @Inject(at = @At(value = "TAIL"), method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    public void addAdditionalSaveData(CompoundTag p_38720_, CallbackInfo info) {
        new EatHistory(queueFood, queueHunger, queueSaturation).toBytes().ifPresent(x->p_38720_.putByteArray(eat_history_label,x));
    }
    @Inject(at = @At(value = "TAIL"), method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    public void readAdditionalSaveData(CompoundTag p_38716_, CallbackInfo info) {
        if (p_38716_.contains(eat_history_label)) {
            Optional.of(p_38716_.getByteArray(eat_history_label))
                    .flatMap(EatHistory::fromBytes)
                    .ifPresent(eatHistory -> {
                        queueFood = eatHistory.food();
                        queueHunger = eatHistory.hunger();
                        queueSaturation = eatHistory.saturation();
                    });
        }
    }
}
