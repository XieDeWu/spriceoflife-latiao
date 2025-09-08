package com.example.examplemod.event;

import com.example.examplemod.Config;
import com.example.examplemod.util.EatFormulaContext;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;

public class PlayerTickEvent {
    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if(player.isCreative() || player.isDeadOrDying()) return;
        Optional<EatFormulaContext> context = EatFormulaContext.from(player, Optional.empty());
        context.ifPresent(x->player.causeFoodExhaustion(x.loss()));
    }
}
