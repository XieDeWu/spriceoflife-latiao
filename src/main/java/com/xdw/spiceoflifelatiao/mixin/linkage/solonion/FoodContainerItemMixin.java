package com.xdw.spiceoflifelatiao.mixin.linkage.solonion;

import com.xdw.spiceoflifelatiao.cached.FoodPropertiesCached;
import com.xdw.spiceoflifelatiao.linkage.IFoodItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(targets = "team.creative.solonion.common.item.foodcontainer.FoodContainerItem")
public abstract class FoodContainerItemMixin implements IFoodItem {
    @Unique
    private ItemStackHandler handler = null;
    @Unique
    private Integer bestFoodSlot = null;
    @Unique
    private Integer duration = 32;
    @Override
    public void initState() {
        handler = null;
        bestFoodSlot = null;
        duration = 32;
    }
    @Inject(
            method = "getUseDuration",
            at = @At("TAIL"),
            cancellable = true,
            remap = false
    )
    private void getUseDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if(!(entity instanceof Player player)) return;
        FoodPropertiesCached.getCached(entity,stack)
                .or(()->getFoodProperties(stack,entity).stream().peek(foodProperties->{
                    FoodPropertiesCached.addCached(player,stack,foodProperties);
                }).findFirst())
                .ifPresent(foodProperties -> {
                    cir.setReturnValue(foodProperties.eatDurationTicks());
                });
    }

    @Override
    public Optional<FoodProperties> getFoodProperties(ItemStack stack, LivingEntity entity){
        Optional<FoodProperties> food = FoodPropertiesCached.getCached(entity,stack);
        if(food.isPresent()) return food;
        if(!(entity instanceof Player player)) return Optional.empty();
        if(!isInventoryEmpty(player,stack) || handler == null || bestFoodSlot == null){
            handler = getInventory(stack);
            if(handler == null) return Optional.empty();
            bestFoodSlot = getBestFoodSlot(handler, player);
        }
        if(handler == null || bestFoodSlot == null || bestFoodSlot < 0) return Optional.empty();
        ItemStack bestFood = handler.getStackInSlot(bestFoodSlot);
        if(bestFood.isEmpty()) return Optional.empty();
        food = Optional.ofNullable(bestFood.getFoodProperties(player));
        return food;
    }

    @Shadow(remap = false)
    public static ItemStackHandler getInventory(ItemStack bag){
        throw new AssertionError();
    }
    @Shadow(remap = false)
    public static int getBestFoodSlot(ItemStackHandler handler, Player player) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private static boolean isInventoryEmpty(Player player, ItemStack container) {
        throw new AssertionError();
    }
}
