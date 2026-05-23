package com.xdw.spiceoflifelatiao.mixin.linkage.farmersdelight;

import com.xdw.spiceoflifelatiao.cached.FoodDataCached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "vectorwing.farmersdelight.common.block.FeastBlock")
public class FeastBlockMixin {

    @Inject(at = @At(value = "HEAD"), method = "takeServing")
    public void takeServingStart(LevelAccessor level, BlockPos pos, BlockState state, Player player, InteractionHand hand, CallbackInfoReturnable<ItemInteractionResult> cir) {
        FoodDataCached.startFeastBlockServingCapture();
    }

    @Inject(at = @At("RETURN"), method = "takeServing")
    public void takeServingEnd(LevelAccessor level, BlockPos pos, BlockState state, Player player, InteractionHand hand, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (cir.getReturnValue() == ItemInteractionResult.SUCCESS && FoodDataCached.feastBlockTakeServing.isPresent()) {
            FoodDataCached.usingConvertsTo = FoodDataCached.feastBlockTakeServing;
        }
        FoodDataCached.endFeastBlockServingCapture();
    }
}
