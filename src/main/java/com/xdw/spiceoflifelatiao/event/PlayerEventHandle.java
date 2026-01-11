package com.xdw.spiceoflifelatiao.event;

import com.xdw.spiceoflifelatiao.Config;
import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.attachments.PlayerUnSleepTimeRecord;
import com.xdw.spiceoflifelatiao.cached.LevelCalcCached;
import com.xdw.spiceoflifelatiao.cached.PlayerAfkCached;
import com.xdw.spiceoflifelatiao.cached.PlayerCalcCached;
import com.xdw.spiceoflifelatiao.cached.PlayerSleepCached;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.Map;

public class PlayerEventHandle {
    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.isCreative() || player.isDeadOrDying()) return;
        if(!Config.EANBLE_CHANGE.get()) return;
        if(!Config.EANBLE_LOSS.get()) return;
        var id = player.getStringUUID().hashCode();
        PlayerAfkCached.addSampling(id,player.getLookAngle(), LevelCalcCached.gameTime);
        if(PlayerAfkCached.isAfk(id, LevelCalcCached.gameTime,PlayerCalcCached.hunger)) return;
        LevelCalcCached.update(player.level());
        PlayerCalcCached.update(player);
        EatFormulaContext.from(player, ItemStack.EMPTY,null).ifPresent(x->player.causeFoodExhaustion(x.loss()));
        var oldData = player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get());

        var newTime = oldData.player_un_sleeptime() + 1;
        if(LevelCalcCached.gameTime % 20 == 0 && player.isSleeping())
            newTime =  Math.round(Math.max(0,newTime*0.98-160));

        player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(), new PlayerUnSleepTimeRecord(newTime));
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
                    player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(),updTime);
                });
    }
}
