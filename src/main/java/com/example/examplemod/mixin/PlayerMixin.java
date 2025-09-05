package com.example.examplemod.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntityMixin {
    @Shadow public abstract FoodData getFoodData();
    @Shadow public abstract void awardStat(Stat<Item> itemStat);

    @Inject(at = @At(value = "HEAD"), method = "Lnet/minecraft/world/entity/player/Player;eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;", cancellable = true)
    public void injected(Level pLevel, ItemStack pFood, FoodProperties pFoodProperties, CallbackInfoReturnable info) {
        info.cancel();
        this.getFoodData().eat(pFoodProperties);
        this.awardStat(Stats.ITEM_USED.get(pFood.getItem()));
        pLevel.playSound(
                null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, pLevel.random.nextFloat() * 0.1F + 0.9F
        );
        if ((Player)(Object)this instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)(Object)this, pFood);
        }

        ItemStack itemstack = super.eat(pLevel, pFood, pFoodProperties);
        Optional<ItemStack> optional = pFoodProperties.usingConvertsTo();
        boolean a = false;
        if (optional.isPresent() && !this.hasInfiniteMaterials()) {
            if (itemstack.isEmpty()) {
                a = true;
            }
            if (!this.level().isClientSide()) {
                ItemStack container = optional.get().copy();
                if (!getInventory().add(container)) {
                    drop(container, false);
                }
            }
        }
        info.setReturnValue( a ? optional.get().copy() : itemstack );
    }
    @Shadow protected abstract boolean hasInfiniteMaterials();
    @Shadow protected abstract Inventory getInventory();
    @Shadow public abstract ItemEntity drop(ItemStack pItemStack, boolean pIncludeThrowerName);
}