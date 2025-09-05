package com.example.examplemod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow protected abstract Level level();
    @Shadow protected abstract double getX();
    @Shadow protected abstract double getY();
    @Shadow protected abstract double getZ();
}