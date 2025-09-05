package com.example.examplemod.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntityMixin {
    @Shadow public abstract FoodData getFoodData();

    @Inject(at = @At(value = "HEAD"), method = "Lnet/minecraft/world/entity/player/Player;eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;", cancellable = true)
    public void injected(Level pLevel, ItemStack pFood, FoodProperties pFoodProperties, CallbackInfoReturnable info) {
        info.cancel();
        FoodData foodData = this.getFoodData();
        ItemStack itemstack = this.eat(pLevel, pFood, pFoodProperties);
        Optional<ItemStack> optional = pFoodProperties.usingConvertsTo();
        if (optional.isPresent() && !this.hasInfiniteMaterials()) {
            if (itemstack.isEmpty()) {
                info.setReturnValue(optional.get().copy());
            }
            if (!this.level().isClientSide()) {
                ItemStack container = optional.get().copy();
                if (!this.getInventory().add(container)) {
                    drop(container, false);
                }
            }
        }
        info.setReturnValue(itemstack);
    }
    @Shadow protected abstract boolean hasInfiniteMaterials();
    @Shadow protected abstract Inventory getInventory();
    @Shadow public abstract ItemEntity drop(ItemStack pItemStack, boolean pIncludeThrowerName);
}