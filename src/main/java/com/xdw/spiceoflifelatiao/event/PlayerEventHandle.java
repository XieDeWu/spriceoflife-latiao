package com.xdw.spiceoflifelatiao.event;

import com.xdw.spiceoflifelatiao.Config;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import com.xdw.spiceoflifelatiao.util.LevelCalcCached;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

public class PlayerEventHandle {
    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.isCreative() || player.isDeadOrDying()) return;
        if(!Config.EANBLE_CHANGE.get()) return;
        EatFormulaContext.from(player, ItemStack.EMPTY).ifPresent(x->player.causeFoodExhaustion(x.loss()));

        LevelCalcCached.update(player);
    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event){
        if(!event.isWasDeath()) return;
        if(!(event.getOriginal().getFoodData() instanceof IEatHistoryAcessor old) || !(event.getEntity().getFoodData() instanceof IEatHistoryAcessor neo )) return;
        old.getEatHistory().ifPresent(neo::setEatHistory);
    }
    private static long time = 0;
    private static float loss = 0f;
    @SubscribeEvent
    public static void canPlayerSleepEvent(CanPlayerSleepEvent event){
        var player = event.getEntity();
        time = player.level().getDayTime();
        loss = EatFormulaContext.from(player,ItemStack.EMPTY).map(EatFormulaContext::loss).orElse(0f);
    }
    @SubscribeEvent
    public static void onPlayerWalkUp(PlayerWakeUpEvent event){
        var player = event.getEntity();
        long dayTime = player.level().getDayTime();
        long gap = Math.clamp(dayTime - time,0,72000);
        float fatigue = gap*loss;
        player.causeFoodExhaustion(fatigue);
        time = dayTime;
        loss = 0f;
    }
}
