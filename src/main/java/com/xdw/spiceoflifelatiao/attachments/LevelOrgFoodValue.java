package com.xdw.spiceoflifelatiao.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public final class LevelOrgFoodValue{
    public final Set<Integer> hash = new HashSet<>();
    public final Map<Integer, Float> hunger = new HashMap<>();
    public final Map<Integer, Float> saturation = new HashMap<>();
    public final Map<Integer, Integer> bites = new HashMap<>();
    public final Map<Integer, Integer> bitesOffset = new HashMap<>();
    public static int getFoodHash(Item item, int bite){
        return (BuiltInRegistries.ITEM.getId(item) << 8) | (bite & 0xFF);
    }
    public static Vec3 getBlockFoodInfo(@NotNull Player player, @NotNull ItemStack stack,int bite,FoodProperties _defaultFoodInfo,boolean sliceCalc,int flag){
        Optional<FoodProperties> defInfo = Optional.ofNullable(_defaultFoodInfo);
            if (!player.isAddedToLevel() || player.tickCount <= 0) return defInfo.map(
                    it -> new Vec3(it.nutrition(), it.saturation(), it.eatSeconds()))
                    .orElse(new Vec3(0, 0, 1.6F));
        LevelOrgFoodValue data = sliceCalc ? player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE) : new LevelOrgFoodValue();
        int hash0 = LevelOrgFoodValue.getFoodHash(stack.getItem(),0);
        float hunger0 = Optional.ofNullable(data.hunger.get(hash0)).orElse((float)defInfo.map(FoodProperties::nutrition).orElse(0));
        float saturation0 = Optional.ofNullable(data.saturation.get(hash0)).orElse(defInfo.map(FoodProperties::saturation).orElse(0F));
        int bites = Optional.ofNullable(data.bites.get(hash0)).orElse(1);
        int bitesOffset = Optional.ofNullable(data.bitesOffset.get(hash0)).orElse(0);
        if(sliceCalc && !data.hash.contains(hash0)){
            if(stack.getItem() instanceof BlockItem bi) {
                BlockState blockState = bi.getBlock().defaultBlockState();
                Optional<Integer> defaultBitesMax = blockState.getValues().values().stream().map(comparable -> {
                    if(comparable instanceof IntegerProperty ip && ip.getName().equals("bites")){
                        try {
                            Field maxField = IntegerProperty.class.getDeclaredField("max");
                            maxField.setAccessible(true);
                            return (int)maxField.get(ip);
                        } catch (Exception ignored) {

                        }
                    }
                    return -1;
                }).filter(it-> it > -1).findFirst();
                if(defaultBitesMax.isPresent()){
                    int defMax = defaultBitesMax.get();
                    bites = defMax;
                    hunger0 = hunger0 / defMax;
                    saturation0 = saturation0 / defMax;
                }
            }
        }
        AtomicReference<Optional<Float>> eat_seconds = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<Float>> hungerAccRoundErr = new AtomicReference<>(Optional.empty());
        Float finalHunger = hunger0;
        Float finalSaturation = saturation0;
        return IntStream.range(bite,Math.max(bite, Math.max(bite,bites +bitesOffset)))
                .mapToObj(it->{
                    var hashIt = LevelOrgFoodValue.getFoodHash(stack.getItem(),it);
                    var tileHunger = Math.round(data.hunger.getOrDefault(hashIt, finalHunger));
                    var tileSaturation = data.saturation.getOrDefault(hashIt, finalSaturation);
                    var calc = EatFormulaContext.from(player,stack,new FoodProperties(
                            tileHunger,
                            tileSaturation,
                            defInfo.map(FoodProperties::canAlwaysEat).orElse(false),
                            defInfo.map(FoodProperties::eatSeconds).orElse(1.6F),
                            defInfo.flatMap(FoodProperties::usingConvertsTo),
                            defInfo.map(FoodProperties::effects).orElse(List.of())
                    ),flag);
                    if(hungerAccRoundErr.get().isEmpty() && calc.isPresent()) hungerAccRoundErr.set(Optional.of(calc.get().hungerAccRoundErr()));
                    if(eat_seconds.get().isEmpty() && calc.isPresent()) eat_seconds.set(Optional.of(calc.get().eat_seconds()));
                    return calc.map(c -> new Vec3(c.hunger(), c.saturation(),0)).orElseGet(() -> new Vec3(0, 0,0));
                })
                .reduce(new Vec3(0,0,0),Vec3::add)
                .add(new Vec3(hungerAccRoundErr.get().orElse(0F),0F,eat_seconds.get().orElse(1.6F)));
    }
    public static boolean checkBlockFoodInfo(@NotNull Player player, @NotNull ItemStack stack){
        return player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE).hash
                .contains(LevelOrgFoodValue.getFoodHash(stack.getItem(),0));
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
                    CustomCodec.INT_INT_MAP.fieldOf("bites").forGetter(v -> v.bites),
                    CustomCodec.INT_INT_MAP.fieldOf("bitesOffset").forGetter(v -> v.bitesOffset)
            ).apply(i, (a, b, c,d,f) -> {
                LevelOrgFoodValue v = new LevelOrgFoodValue();
                v.hash.addAll(a);
                v.hunger.putAll(b);
                v.saturation.putAll(c);
                v.bites.putAll(d);
                v.bitesOffset.putAll(f);
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

                        b.writeVarInt(v.bitesOffset.size());
                        v.bitesOffset.forEach((k, f) -> {
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

                        for (int i = b.readVarInt(); i-- > 0; )
                            v.bitesOffset.put(b.readVarInt(), b.readInt());

                        return v;
                    }
            );

}
