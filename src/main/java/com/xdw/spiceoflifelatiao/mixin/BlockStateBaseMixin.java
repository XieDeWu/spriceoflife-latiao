package com.xdw.spiceoflifelatiao.mixin;

import com.xdw.spiceoflifelatiao.cached.BlockBehaviourCached;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow protected abstract BlockState asState();

    @Inject(at = @At(value = "HEAD"), method = "useWithoutItem")
    public void useWithoutItemBefore(Level level, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockBehaviourCached.start(Optional.ofNullable(player),Optional.of(asState().getBlock().asItem().getDefaultInstance()));
    }
    @Inject(at = @At(value = "TAIL"), method = "useWithoutItem")
    public void useWithoutItemAfter(Level level, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockBehaviourCached.end();
    }
}
