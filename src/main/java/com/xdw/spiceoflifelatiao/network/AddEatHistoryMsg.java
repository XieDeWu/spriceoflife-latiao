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

public record AddEatHistoryMsg(int foodID, float hunger, float saturation, float eaten, float hungerRoundErr) implements CustomPacketPayload
{
    public static final Type<AddEatHistoryMsg> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiceOfLifeLatiao.MODID, "add_eat_history"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddEatHistoryMsg> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            AddEatHistoryMsg::foodID,
            ByteBufCodecs.FLOAT,
            AddEatHistoryMsg::hunger,
            ByteBufCodecs.FLOAT,
            AddEatHistoryMsg::saturation,
            ByteBufCodecs.FLOAT,
            AddEatHistoryMsg::eaten,
            ByteBufCodecs.FLOAT,
            AddEatHistoryMsg::hungerRoundErr,
            AddEatHistoryMsg::new
    );
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(final AddEatHistoryMsg message, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ((IEatHistoryAcessor)(ctx.player().getFoodData())).addEatHistory_Mem(
                    message.foodID, message.hunger, message.saturation, message.eaten, message.hungerRoundErr
            );
        });
    }
}
