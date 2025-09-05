package com.example.examplemod.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Optional;

@Mixin(IItemExtension.class)
public interface IItemExtensionMixin {
    /**
     * @author xdw
     * @reason 直接对Food组件读取值进行变换，简单强兼容
     */
    @Overwrite
    @Nullable // read javadoc to find a potential problem
    default FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        var food = stack.get(DataComponents.FOOD);
        if(food == null) return food;
        int nutrition = Math.min(food.nutrition() + 10,20);
        float saturation = Math.min(food.saturation() + 10,20);
        boolean canAlwaysEat = food.canAlwaysEat();
        float eatSeconds = food.eatSeconds();
        Optional<ItemStack> usingConvertsTo = food.usingConvertsTo();
        List<FoodProperties.PossibleEffect> effects = food.effects();
        FoodProperties modifyFood = new FoodProperties(nutrition,saturation,canAlwaysEat,eatSeconds,usingConvertsTo,effects);
        return modifyFood;
    }
}
