package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.cached.BlockBehaviourCached;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Optional;

@Mixin(StateHolder.class)
public class StateHolderMixin {
    @Inject(at = @At("HEAD"),method = "getValue")
    private static <T extends Comparable<T>> void getValue(Property<T> property, CallbackInfoReturnable<T> cir) {
        if(BlockBehaviourCached.flag
//                && Integer.class.isAssignableFrom(property.getValueClass())
                && property instanceof IntegerProperty intProp
                && property.getName().equals("bites")
        ){
            try {
                Field maxField = IntegerProperty.class.getDeclaredField("max");
                maxField.setAccessible(true);
                int max = (int) maxField.get(intProp);
                BlockBehaviourCached.bites = Optional.of(max);
            } catch (Exception ignored) {

            }
        }
    }
}
