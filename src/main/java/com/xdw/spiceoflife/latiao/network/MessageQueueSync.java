package com.xdw.spiceoflife.latiao.network;

import com.xdw.spiceoflife.latiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflife.latiao.util.EatHistoryAcessor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MessageQueueSync(byte[] eatHistory) implements CustomPacketPayload
{
    public static final Type<MessageQueueSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiceOfLifeLatiao.MODID, "eat_history"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageQueueSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY,
            MessageQueueSync::eatHistory,
            MessageQueueSync::new
    );


    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final MessageQueueSync message, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ((EatHistoryAcessor)(ctx.player().getFoodData())).setEatHistory(message.eatHistory());
        });
    }
}