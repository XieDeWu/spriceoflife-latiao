package com.xdw.spiceoflifelatiao.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.*;

public final class LevelOrgFoodValue{
    public final Set<Integer> hash = new HashSet<>();
    public final Map<Integer, Float> hunger = new HashMap<>();
    public final Map<Integer, Float> saturation = new HashMap<>();
    public final Map<Integer, Integer> bites = new HashMap<>();
    public static int getFoodHash(Item item, int bite){
        return (BuiltInRegistries.ITEM.getId(item) << 8) | (bite & 0xFF);
    }

    static final class CustomCodec {

        public static <K, V> Codec<Map<K, V>> mapAsList(
                Codec<K> keyCodec,
                Codec<V> valueCodec
        ) {
            Codec<Map.Entry<K, V>> entryCodec =
                    RecordCodecBuilder.create(inst -> inst.group(
                            keyCodec.fieldOf("k").forGetter(Map.Entry::getKey),
                            valueCodec.fieldOf("v").forGetter(Map.Entry::getValue)
                    ).apply(inst, Map::entry));

            return Codec.list(entryCodec)
                    .xmap(
                            list -> {
                                Map<K, V> map = new HashMap<>();
                                for (var e : list) map.put(e.getKey(), e.getValue());
                                return map;
                            },
                            map -> map.entrySet().stream().toList()
                    );
        }
        static final Codec<Map<Integer, Float>> INT_FLOAT_MAP =
                CustomCodec.mapAsList(Codec.INT, Codec.FLOAT);

        static final Codec<Map<Integer, Integer>> INT_INT_MAP =
                CustomCodec.mapAsList(Codec.INT, Codec.INT);
    }


    // ===== Codec（存档） =====
    public static final Codec<LevelOrgFoodValue> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.listOf().fieldOf("hash").forGetter(v -> v.hash.stream().toList()),
                    CustomCodec.INT_FLOAT_MAP.fieldOf("hunger").forGetter(v -> v.hunger),
                    CustomCodec.INT_FLOAT_MAP.fieldOf("saturation").forGetter(v -> v.saturation),
                    CustomCodec.INT_INT_MAP.fieldOf("bites").forGetter(v -> v.bites)
            ).apply(i, (a, b, c,d) -> {
                LevelOrgFoodValue v = new LevelOrgFoodValue();
                v.hash.addAll(a);
                v.hunger.putAll(b);
                v.saturation.putAll(c);
                v.bites.putAll(d);
                return v;
            }));

    // ===== StreamCodec（网络同步） =====
    public static final StreamCodec<FriendlyByteBuf, LevelOrgFoodValue> STREAM_CODEC =
            StreamCodec.of(
                    (b, v) -> {
                        b.writeVarInt(v.hash.size());
                        v.hash.forEach(b::writeVarInt);

                        b.writeVarInt(v.hunger.size());
                        v.hunger.forEach((k, f) -> {
                            b.writeVarInt(k);
                            b.writeFloat(f);
                        });

                        b.writeVarInt(v.saturation.size());
                        v.saturation.forEach((k, f) -> {
                            b.writeVarInt(k);
                            b.writeFloat(f);
                        });

                        b.writeVarInt(v.bites.size());
                        v.bites.forEach((k, f) -> {
                            b.writeVarInt(k);
                            b.writeInt(f);
                        });
                    },
                    b -> {
                        LevelOrgFoodValue v = new LevelOrgFoodValue();

                        for (int i = b.readVarInt(); i-- > 0; )
                            v.hash.add(b.readVarInt());

                        for (int i = b.readVarInt(); i-- > 0; )
                            v.hunger.put(b.readVarInt(), b.readFloat());

                        for (int i = b.readVarInt(); i-- > 0; )
                            v.saturation.put(b.readVarInt(), b.readFloat());

                        for (int i = b.readVarInt(); i-- > 0; )
                            v.bites.put(b.readVarInt(), b.readInt());

                        return v;
                    }
            );

}
