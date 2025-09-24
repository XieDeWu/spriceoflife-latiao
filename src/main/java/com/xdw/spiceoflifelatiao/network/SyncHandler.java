package com.xdw.spiceoflifelatiao.network;

import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.event.PlayerEventHandle;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

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
        syncEatHistory(event.getEntity(),Optional.empty());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event){
        syncEatHistory(event.getOriginal(), Optional.of(event.getEntity()));
    }

    @SubscribeEvent
    public void onPlayerChangedDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event){
        syncEatHistory(event.getEntity(),Optional.empty());
    }

    @SubscribeEvent
    public void onPlayerRespawnEvent(PlayerEvent.PlayerRespawnEvent event){
        syncEatHistory(event.getEntity(),Optional.empty());
    }

    private static void syncEatHistory(Player _oldPlayer, Optional<Player> _newPlayer) {
        _newPlayer.ifPresent(x->{
            if(!(_oldPlayer.getFoodData() instanceof IEatHistoryAcessor old) || !(x.getFoodData() instanceof IEatHistoryAcessor neo )) return;
            old.getEatHistory().ifPresent(neo::setEatHistory);
        });
        if (!(_oldPlayer instanceof ServerPlayer player)) return;
        FoodData foodData = player.getFoodData();
        if (foodData instanceof IEatHistoryAcessor){
            ((IEatHistoryAcessor)(foodData))
                    .getEatHistory()
                    .ifPresent(x->{
                        PacketDistributor.sendToPlayer(player, new MessageQueueSync(x));
                    });
        }
    }
}
