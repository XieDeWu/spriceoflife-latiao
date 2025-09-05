package com.example.examplemod.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IItemExtension.class)
public interface IItemExtensionMixin {
    @Overwrite
    @Nullable // read javadoc to find a potential problem
    default FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return stack.get(DataComponents.FOOD);
    }
}
