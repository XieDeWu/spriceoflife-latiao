package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PlayerCalcCached {
    public static float light = 0f;
    public static float rainLevel = 0f;
    public static float thunderLevel = 0f;
    public static long player_un_sleeptime = 0L;
    public static int hunger = 0;
    private static final FifoHashMap<Long,Integer> skyLightCached = new FifoHashMap<>(64);
    private static final FifoHashMap<Long,Integer> blockLightCached = new FifoHashMap<>(64);
    private static final FifoHashMap<Long,Boolean> canSeeSkyCached = new FifoHashMap<>(64);
    public static void update(Player player){
        Level level = player.level();
        rainLevel = LevelCalcCached.rainLevel;
        thunderLevel = LevelCalcCached.thunderLevel;
        player_un_sleeptime = player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get()).player_un_sleeptime();
        hunger = player.getFoodData().getFoodLevel();
        var pos = player.blockPosition();
        var posKey = getPosKey(player.getUUID(),pos);
        float baseSkyLight = Optional.ofNullable(skyLightCached.get(posKey)).orElseGet(()->{
            var light = level.getBrightness(LightLayer.SKY, pos);
            skyLightCached.put(posKey,light);
            return light;
        });
        float blockLight = Optional.ofNullable(blockLightCached.get(posKey)).orElseGet(()->{
            var light = level.getBrightness(LightLayer.BLOCK, pos);
            blockLightCached.put(posKey,light);
            return light;
        });
        var canSee = Optional.ofNullable(canSeeSkyCached.get(posKey)).orElseGet(()->{
            var can = level.canSeeSky(pos);
            canSeeSkyCached.put(posKey,can);
            return can;
        });
        float dayTime = (float) (LevelCalcCached.timeOfDay+0.25)%1f;
        double day = dayTime * Math.PI * 2;
        double night = day + Math.PI;
        if(!canSee){
            float scale = 1/(16-baseSkyLight);
            rainLevel *= scale;
            thunderLevel *= scale;
        }
        float skyLight = (float) (baseSkyLight / (1+rainLevel+thunderLevel) * Math.max(Math.sin(day),0.6 * LevelCalcCached.moonBrightness *Math.sin(night)));
        Float itemLight = Stream.of(player.getItemInHand(InteractionHand.MAIN_HAND), player.getItemInHand(InteractionHand.OFF_HAND))
                .filter(Predicate.not(ItemStack::isEmpty))
                .map(ItemStack::getItem)
                .map(PlayerCalcCached::getItemLight)
                .max(Integer::compare)
                .map(it->(float)it)
                .orElse(0f);
        light = Stream.of(blockLight,skyLight,0.6f*itemLight).max(Float::compare).orElse(0f);
    }

    private static long getPosKey(UUID playerId, BlockPos pos) {
        long p = pos.asLong();
        long u = playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
        return p ^ (u * 31L);
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
