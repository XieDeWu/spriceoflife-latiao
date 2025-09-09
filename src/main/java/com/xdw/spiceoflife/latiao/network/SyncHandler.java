package com.xdw.spiceoflife.latiao.network;

import com.xdw.spiceoflife.latiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflife.latiao.util.EatHistoryAcessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class SyncHandler {
    public static void onRegisterPayloadHandler(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar registrar = event.registrar(SpiceOfLifeLatiao.MODID)
                .versioned(SpiceOfLifeLatiao.VERSION)
                .optional();
        registrar.playToClient(MessageQueueSync.TYPE, MessageQueueSync.CODEC, MessageQueueSync::handle);
        NeoForge.EVENT_BUS.register(new SyncHandler());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        FoodData foodData = player.getFoodData();
        if (foodData instanceof EatHistoryAcessor){
            ((EatHistoryAcessor)(foodData))
                    .getEatHistory()
                    .ifPresent(x->{
                        PacketDistributor.sendToPlayer(player, new MessageQueueSync(x));
                    });
        }
    }
}
