package com.xdw.spiceoflifelatiao.attachments;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PlayerUnSleepTimeRecord(long player_un_sleeptime) {
    // ===== StreamCodec（服务端 → 客户端同步） =====
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerUnSleepTimeRecord> STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> buf.writeLong(data.player_un_sleeptime),
                    buf -> new PlayerUnSleepTimeRecord(buf.readLong())
            );
    // ===== Codec（存档） =====
    public static final Codec<PlayerUnSleepTimeRecord> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.LONG.fieldOf("player_un_sleeptime").forGetter(PlayerUnSleepTimeRecord::player_un_sleeptime)
            ).apply(inst, PlayerUnSleepTimeRecord::new));
}