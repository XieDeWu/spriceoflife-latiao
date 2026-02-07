package com.xdw.spiceoflifelatiao.network;

import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EatHistoryMsg(byte[] eatHistory) implements CustomPacketPayload
{
    public static final Type<EatHistoryMsg> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiceOfLifeLatiao.MODID, "eat_history"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EatHistoryMsg> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY,
            EatHistoryMsg::eatHistory,
            EatHistoryMsg::new
    );


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final EatHistoryMsg message, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> ((IEatHistoryAcessor)(ctx.player().getFoodData())).setEatHistory_Bin(message.eatHistory()));
    }
}