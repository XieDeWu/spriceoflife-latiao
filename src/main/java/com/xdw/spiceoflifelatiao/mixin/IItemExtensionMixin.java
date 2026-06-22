package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.cached.FoodDataCached;
import com.xdw.spiceoflifelatiao.cached.FoodPropertiesCached;
import com.xdw.spiceoflifelatiao.cached.LevelCalcCached;
import com.xdw.spiceoflifelatiao.linkage.IFoodItem;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(IItemExtension.class)
public interface IItemExtensionMixin {
    /**
     * @author xdw
     * @reason 直接对Food组件读取值进行变换，简单强兼容
     */
    @Overwrite
    @Nullable
    default FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        FoodDataCached.readFoodInfo = true;
        if (entity != null) EatHistory.recentEntity = Optional.of(entity);
        FoodProperties base = spiceoflife_latiao$resolveBaseFood(stack, entity);
        if (!ConfigCached.ENABLE_CHANGE) return base;
        Player player = (Player) EatHistory.recentEntity.filter(e -> e instanceof Player).orElse(null);
        if( player == null || base == null) return base;
        return spiceoflife_latiao$applyModifications(stack, player, base);
    }

    @Unique
    private static FoodProperties spiceoflife_latiao$resolveBaseFood(ItemStack stack, LivingEntity entity) {
        Optional<FoodProperties> cached = FoodPropertiesCached.getCached(entity, stack);
        if (cached.isPresent()) return cached.get();
        FoodProperties food = stack.getItem().getDefaultInstance().get(DataComponents.FOOD);
        if (EatHistory.recentEntity.orElse(null) instanceof Player player) {
            if (stack.getItem() instanceof IFoodItem box) food = box.getFoodProperties(stack, player).orElse(food);
            if (food != null) FoodPropertiesCached.addCached(player, stack, food);
        }
        return food;
    }

    @Unique
    private static FoodProperties spiceoflife_latiao$applyModifications(ItemStack stack, @Nullable Player player,
                                                                        FoodProperties base) {
        int nutrition = 0;
        float saturation = 0;
        float eatSeconds = 1.6F;
        if (player != null) {
            int count = stack.getCount();
            stack.setCount(1);
            Vec3 v = LevelOrgFoodValue.getBlockFoodInfo(player, stack, null, null, null, base, true,
                    (int) LevelCalcCached.gameTime);
            stack.setCount(count);
            nutrition = (int) Math.round(v.x());
            saturation = (float) v.y;
            eatSeconds = (float) v.z;
            FoodDataCached.hungerRoundErr = Optional.of((float) (v.x() - nutrition));
        }
        if (nutrition == 0 && saturation == 0 && base.usingConvertsTo().isEmpty() && base.effects().isEmpty())
            return base;
        return new FoodProperties(nutrition, saturation, base.canAlwaysEat(), eatSeconds, base.usingConvertsTo(),
                base.effects());
    }
}