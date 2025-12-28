package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.Config;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        if(food == null) return null;
        if(!Config.EANBLE_CHANGE.get()) return food;
        if(entity != null) EatHistory.recentEntity = Optional.of(entity);
        AtomicInteger nutrition = new AtomicInteger(food.nutrition());
        AtomicReference<Float> saturation = new AtomicReference<>(food.saturation());
        AtomicReference<Float> eatSeconds = new AtomicReference<>(food.eatSeconds());
        int count = stack.getCount();
        stack.setCount(Math.max(1,stack.getCount()));
        EatHistory.recentEntity
                .map(x-> x instanceof Player p ? p : null)
                .flatMap(rp -> EatFormulaContext.from(rp, stack)).ifPresent(x -> {
                    nutrition.set(new BigDecimal(x.hunger()+x.hungerAccRoundErr()).setScale(0, RoundingMode.HALF_EVEN).intValue());
                    saturation.set(x.saturation());
                    eatSeconds.set(x.eat_seconds());
                });
        boolean canAlwaysEat = food.canAlwaysEat();
        Optional<ItemStack> usingConvertsTo = food.usingConvertsTo();
        List<FoodProperties.PossibleEffect> effects = food.effects();
        stack.setCount(count);
        return new FoodProperties(nutrition.get(), saturation.get(),canAlwaysEat, eatSeconds.get(),usingConvertsTo,effects);
    }
}
