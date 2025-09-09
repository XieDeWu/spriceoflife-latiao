package com.xdw.spiceoflife.latiao.event;

import com.xdw.spiceoflife.latiao.Config;
import com.xdw.spiceoflife.latiao.util.EatFormulaContext;
import com.xdw.spiceoflife.latiao.util.EatHistoryAcessor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

public class PlayerEventHandle {
    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.isCreative() || player.isDeadOrDying()) return;
        if(!Config.EANBLE.get()) return;
        EatFormulaContext.from(player, Optional.empty()).ifPresent(x->player.causeFoodExhaustion(x.loss()));
    }
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event){
        if(!event.isWasDeath()) return;
        if(!(event.getOriginal().getFoodData() instanceof EatHistoryAcessor old) || !(event.getEntity().getFoodData() instanceof EatHistoryAcessor neo )) return;
        old.getEatHistory().ifPresent(neo::setEatHistory);
    }
}
