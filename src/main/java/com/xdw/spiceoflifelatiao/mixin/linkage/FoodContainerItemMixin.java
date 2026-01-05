package com.xdw.spiceoflifelatiao.mixin.linkage;

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
        if(handler == null || bestFoodSlot == null){
            handler = getInventory(stack);
            if(handler == null) return;
            bestFoodSlot = getBestFoodSlot(handler, player);
            if(bestFoodSlot < 0) return;
            ItemStack bestFood = handler.getStackInSlot(bestFoodSlot);
            if(bestFood == null || bestFood.isEmpty()) return;
            FoodProperties foodProperties = bestFood.getFoodProperties(player);
            duration = foodProperties != null ? foodProperties.eatDurationTicks() : cir.getReturnValue();
        }
        cir.setReturnValue(duration);
    }

    @Shadow(remap = false)
    public static ItemStackHandler getInventory(ItemStack bag){
        throw new AssertionError();
    }
    @Shadow(remap = false)
    public static int getBestFoodSlot(ItemStackHandler handler, Player player) {
        throw new AssertionError();
    }

}
