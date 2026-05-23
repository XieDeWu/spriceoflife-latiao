package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.cached.FoodDataCached;
import com.xdw.spiceoflifelatiao.config.ManualFoodConfig;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(at = @At("HEAD"), method = "getUseDuration", cancellable = true)
    public void getUseDurationStart(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        EatHistory.recentEntity = Optional.ofNullable(entity);
        if (stack.get(DataComponents.FOOD) != null) return;
        manualFood(entity == null ? null : entity.level(), stack).ifPresent(food -> cir.setReturnValue(Math.max(1, Math.round(food.eatSeconds() * 20.0F))));
    }

    @Inject(at = @At("HEAD"), method = "getUseAnimation", cancellable = true)
    public void getUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        if (stack.get(DataComponents.FOOD) != null) return;
        if (ConfigCached.ENABLE_MANUAL_FOOD_FILE && ManualFoodConfig.hasAnyEnabledEntry(stack.getItem())) {
            cir.setReturnValue(UseAnim.EAT);
        }
    }

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    public void use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.get(DataComponents.FOOD) != null) return;
        Optional<FoodProperties> food = manualFood(level, stack);
        if (food.isEmpty()) return;

        if (player.canEat(food.get().canAlwaysEat())) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResultHolder.consume(stack));
        } else {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }

    @Inject(
            method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    public void finishManualFood(ItemStack stack, Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.get(DataComponents.FOOD) != null) return;
        manualFood(level, stack).ifPresent(food -> cir.setReturnValue(livingEntity.eat(level, stack, food)));
    }

    @Inject(
            method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"),
            require = 0
    )
    public void end(ItemStack stack, Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
        FoodDataCached.end();
        FoodDataCached.flag_common = false;
    }

    private Optional<FoodProperties> manualFood(Level level, ItemStack stack) {
        if (!ConfigCached.ENABLE_MANUAL_FOOD_FILE) return Optional.empty();
        return ManualFoodConfig.getFoodProperties(level, stack, null);
    }
}
