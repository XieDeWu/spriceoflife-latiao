package com.xdw.spiceoflifelatiao.event;

import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.attachments.PlayerUnSleepTimeRecord;
import com.xdw.spiceoflifelatiao.cached.*;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.Map;
import java.util.Optional;

public class PlayerEventHandle {
    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.isCreative() || player.isDeadOrDying()) return;
        if(!ConfigCached.EANBLE_CHANGE) return;
        if(!ConfigCached.EANBLE_LOSS) return;
        var id = player.getStringUUID().hashCode();
        PlayerAfkCached.addSampling(id,player.getLookAngle(), LevelCalcCached.gameTime);
        if(PlayerAfkCached.isAfk(id, LevelCalcCached.gameTime,PlayerCalcCached.hunger)) return;
        LevelCalcCached.update(player.level());
        PlayerCalcCached.update(player);
        EatFormulaContext.from(player, ItemStack.EMPTY,null).ifPresent(x->player.causeFoodExhaustion(x.loss()));

        if(LevelCalcCached.gameTime % 20 == 0){
            long oldTime = player.level().isClientSide
                    ? player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get()).player_un_sleeptime()
                    : Optional.ofNullable(ModAttachments.server_cached_player_un_sleeptime.get(id))
                    .orElseGet(()->player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get()).player_un_sleeptime());
            var newTime = oldTime + 1;
            newTime = player.isSleeping() ? Math.round(Math.max(0,newTime*0.98-160)) : newTime;
            if(player.level().isClientSide){
                player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(), new PlayerUnSleepTimeRecord(newTime));
            }else{
                ModAttachments.server_cached_player_un_sleeptime.put(id,newTime);
                if(LevelCalcCached.gameTime % 200 == 0) {
                    player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(), new PlayerUnSleepTimeRecord(newTime));
                }
            }
        }
    }


    @SubscribeEvent
    public static void canPlayerSleepEvent(CanPlayerSleepEvent event){
        var player = event.getEntity();
        var id = player.getStringUUID().hashCode();
        var time = player.level().getDayTime();
        var loss = EatFormulaContext.from(player,ItemStack.EMPTY,null).map(EatFormulaContext::loss).orElse(0f);
        PlayerSleepCached.addSampling(id,time,loss);
    }
    @SubscribeEvent
    public static void onPlayerWalkUp(PlayerWakeUpEvent event){
        var player = event.getEntity();
        var id = player.getStringUUID().hashCode();
        PlayerSleepCached
                .getTime(id).flatMap(time -> PlayerSleepCached.getLoss(id).map(loss -> Map.entry(time, loss)))
                .ifPresent(entry -> {
                    long dayTime = player.level().getDayTime();
                    long gap = Math.clamp(dayTime - entry.getKey(), 0, 72000);
                    float fatigue = gap * entry.getValue();
                    player.causeFoodExhaustion(fatigue);

                    var time = player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get());
                    var updTime = new PlayerUnSleepTimeRecord(Math.round(Math.max(0,time.player_un_sleeptime() * 0.7 - gap * 6)));
                    ModAttachments.server_cached_player_un_sleeptime.put(id,updTime.player_un_sleeptime());
                    player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(),updTime);
                });
    }
}
