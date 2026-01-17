package com.xdw.spiceoflifelatiao.mixin.linkage.kaleidoscopecookery;

import com.xdw.spiceoflifelatiao.linkage.IFoodItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.TransmutationLunchBagItem")
public class TransmutationLunchBagItemMixin implements IFoodItem {
    @Override
    public Optional<FoodProperties> getFoodProperties(ItemStack stack, LivingEntity entity) {
        if (!hasItems(stack)) {
            return Optional.empty();
        } else {
            ItemStackHandler items = getItems(stack);
            for(int i = 0; i < items.getSlots(); ++i) {
                ItemStack stackInSlot = items.getStackInSlot(i);
                if (!stackInSlot.isEmpty()) {
                    FoodProperties foodProperties = stackInSlot.getItem().getFoodProperties(stackInSlot, entity);
                    return Optional.ofNullable(foodProperties);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void initState() {}

    @Shadow
    public static boolean hasItems(ItemStack bag) {
        throw new AssertionError();
    }

    @Shadow
    public static ItemStackHandler getItems(ItemStack bag) {
        throw new AssertionError();
    }
}
