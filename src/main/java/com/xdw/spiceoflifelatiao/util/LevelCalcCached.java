package com.xdw.spiceoflifelatiao.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class LevelCalcCached {
    public static float light = 0f;
    public static float rainLevel = 0f;
    public static float thunderLevel = 0f;
    public static void update(Player player){
        Level level = player.level();
        rainLevel = level.getRainLevel(0);
        thunderLevel = level.getThunderLevel(0);
        float dayTime = (float) (level.getTimeOfDay(0)+0.25)%1f;
        double day = dayTime * Math.PI * 2;
        double night = day + Math.PI;
        float baseSkyLight = level.getBrightness(LightLayer.SKY, player.blockPosition());
        float skyLight = (float) (baseSkyLight / (1+rainLevel+thunderLevel) * Math.max(Math.sin(day),0.6 * level.getMoonBrightness() *Math.sin(night)));

        float blockLight = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
        Float itemLight = Stream.of(player.getItemInHand(InteractionHand.MAIN_HAND), player.getItemInHand(InteractionHand.OFF_HAND))
                .filter(Predicate.not(ItemStack::isEmpty))
                .map(ItemStack::getItem)
                .map(LevelCalcCached::getItemLight)
                .max(Integer::compare)
                .map(it->(float)it)
                .orElse(0f);
        light = Stream.of(blockLight,skyLight,0.6f*itemLight).max(Float::compare).orElse(0f);
    }

    @SuppressWarnings("deprecation")
    private static int getItemLight(Item item){
        if(item instanceof BlockItem blockItem){
            return blockItem.getBlock().defaultBlockState().getLightEmission();
        }else if(item instanceof BucketItem bucketItem && bucketItem.content == Fluids.LAVA){
            return 15;
        }
        return 0;
    }
}
