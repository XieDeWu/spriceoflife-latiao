package com.xdw.spiceoflifelatiao.mixin.linkage.farmersdelight;

import com.xdw.spiceoflifelatiao.linkage.farmersdelight.FeastBlockCached;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Inject(at = @At(value = "HEAD"), method = "add*")
    public void add(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if(!FeastBlockCached.flag) return;
        FeastBlockCached.takeServing = Optional.of(stack.copy());
    }
}
