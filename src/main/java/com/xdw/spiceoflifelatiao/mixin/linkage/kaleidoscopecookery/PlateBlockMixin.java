package com.xdw.spiceoflifelatiao.mixin.linkage.kaleidoscopecookery;

import com.xdw.spiceoflifelatiao.cached.FoodDataCached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock")
public abstract class PlateBlockMixin {
    @Final
    @Shadow
    protected List<Supplier<Item>> items;

    @Inject(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;" +
            "setBlockAndUpdate(Lnet/minecraft/core/BlockPos;" + "Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void hookStateChanged(ItemStack itemInHand, BlockState state, Level level, BlockPos pos, Player player,
                               InteractionHand hand, BlockHitResult hitResult,
                               CallbackInfoReturnable<ItemInteractionResult> cir) {
        FoodDataCached.usingConvertsTo = Optional.of(items.stream().map(it -> it.get().getDefaultInstance()).toList());
    }
}
