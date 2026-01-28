package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.cached.FoodPropertiesCached;
import com.xdw.spiceoflifelatiao.linkage.IFoodItem;
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
        if (entity != null) EatHistory.recentEntity = Optional.of(entity);
        AtomicReference<FoodProperties> food = new AtomicReference<>();
        FoodPropertiesCached.getCached(entity, stack)
                .or(() -> {
                    AtomicReference<Optional<FoodProperties>> f = new AtomicReference<>(Optional.ofNullable(stack.get(DataComponents.FOOD)));
                    if (EatHistory.recentEntity.isEmpty()) return f.get();
                    if (!(EatHistory.recentEntity.get() instanceof Player player)) return f.get();
//                   其他来源，如饭盒
                    if (stack.getItem() instanceof IFoodItem box) {
                        f.set(box.getFoodProperties(stack, player));
                    }
                    f.get().ifPresent(it -> FoodPropertiesCached.addCached(player, stack, it));
                    return f.get();
                })
                .ifPresent(food::set);
        if (!ConfigCached.EANBLE_CHANGE) return food.get();
        if(food.get() == null
                && EatHistory.recentEntity.isPresent()
                && EatHistory.recentEntity.get() instanceof Player _p
                && !LevelOrgFoodValue.checkBlockFoodInfo(_p,stack)
        ) return null;
        AtomicInteger nutrition = new AtomicInteger(0);
        AtomicReference<Float> saturation = new AtomicReference<>(0F);
        AtomicReference<Float> eatSeconds = new AtomicReference<>(1.6F);
        int count = stack.getCount();
        stack.setCount(Math.max(1, stack.getCount()));
        EatHistory.recentEntity
                .map(x -> x instanceof Player p ? p : null)
                .map(rp -> LevelOrgFoodValue.getBlockFoodInfo(rp,stack,food.get(),0))
                .ifPresent(vec3 -> {
                    nutrition.set((int) Math.round(vec3.x()));
                    saturation.set((float) vec3.y);
                    eatSeconds.set((float) vec3.z);
                });
        boolean canAlwaysEat = Optional.ofNullable(food.get()).map(FoodProperties::canAlwaysEat).orElse(false);
        Optional<ItemStack> usingConvertsTo = Optional.ofNullable(food.get()).flatMap(FoodProperties::usingConvertsTo);
        List<FoodProperties.PossibleEffect> effects = Optional.ofNullable(food.get()).map(FoodProperties::effects).orElse(List.of());
        stack.setCount(count);
        return new FoodProperties(nutrition.get(), saturation.get(), canAlwaysEat, eatSeconds.get(), usingConvertsTo, effects);
    }
}
