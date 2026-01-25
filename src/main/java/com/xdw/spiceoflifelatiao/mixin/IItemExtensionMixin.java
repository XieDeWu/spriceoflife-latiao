package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.cached.BlockBehaviourCached;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.cached.FoodPropertiesCached;
import com.xdw.spiceoflifelatiao.linkage.IFoodItem;
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
        AtomicReference<Optional<Integer>> bites = new AtomicReference<>(Optional.empty());
        FoodPropertiesCached.getCached(entity, stack)
                .or(() -> {
                    AtomicReference<Optional<FoodProperties>> f = new AtomicReference<>(Optional.ofNullable(stack.get(DataComponents.FOOD)));
                    if (EatHistory.recentEntity.isEmpty()) return f.get();
                    if (!(EatHistory.recentEntity.get() instanceof Player player)) return f.get();
//                    统一食物原始数值表 这样就不用区分普通食物 方块食物 多方块食物了
                    if(f.get().isEmpty()){
                        var orgHash = LevelOrgFoodValue.getFoodHash(stack.getItem(),0);
                        var data = player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
                        if(data.hash.contains(orgHash)){
                            var hunger = data.hunger.getOrDefault(orgHash,0F);
                            var saturation = data.saturation.getOrDefault(orgHash,0F);
                            bites.set(Optional.ofNullable(data.bites.getOrDefault(orgHash, 1)));
                            f.set(Optional.of(new FoodProperties(
                                    Math.round(hunger),
                                    saturation,
                                    false,
                                    1.6F,
                                    Optional.empty(),
                                    List.of()
                            )));
                        }
                    }
                    if (stack.getItem() instanceof IFoodItem box) {
                        f.set(box.getFoodProperties(stack, player));
                        f.get().ifPresent(it -> FoodPropertiesCached.addCached(player, stack, it));
                    }
                    return f.get();
                })
                .ifPresent(food::set);
        if (food.get() == null) return null;
        if (!ConfigCached.EANBLE_CHANGE) return food.get();
        AtomicInteger nutrition = new AtomicInteger(food.get().nutrition());
        AtomicReference<Float> saturation = new AtomicReference<>(food.get().saturation());
        AtomicReference<Float> eatSeconds = new AtomicReference<>(food.get().eatSeconds());
        int count = stack.getCount();
        stack.setCount(Math.max(1, stack.getCount()));
        EatHistory.recentEntity
                .map(x -> x instanceof Player p ? p : null)
                .flatMap(rp -> EatFormulaContext.from(rp, stack, food.get()))
                .ifPresent(x -> {
                    nutrition.set(Math.round(x.hunger() * bites.get().orElse(1) + x.hungerAccRoundErr()));
                    saturation.set(x.saturation() * bites.get().orElse(1));
                    eatSeconds.set(x.eat_seconds());
                });
        boolean canAlwaysEat = food.get().canAlwaysEat();
        Optional<ItemStack> usingConvertsTo = food.get().usingConvertsTo();
        List<FoodProperties.PossibleEffect> effects = food.get().effects();
        stack.setCount(count);
        return new FoodProperties(nutrition.get(), saturation.get(), canAlwaysEat, eatSeconds.get(), usingConvertsTo, effects);
    }
}
