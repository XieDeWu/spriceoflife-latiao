package com.example.examplemod.event;

import com.example.examplemod.Config;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

public class PlayerTickEvent {
    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.isCreative() && !player.isDeadOrDying()) {
            player.causeFoodExhaustion(0.01f);
        }
    }
}
