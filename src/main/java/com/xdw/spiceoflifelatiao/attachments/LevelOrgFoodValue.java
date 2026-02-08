package com.xdw.spiceoflifelatiao.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
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

public final class LevelOrgFoodValue {
    public final Set<Integer> hash = new HashSet<>();
    public final Map<Integer, Float> hunger = new HashMap<>();
    public final Map<Integer, Float> saturation = new HashMap<>();
    public final Map<Integer, Integer> bites = new HashMap<>();
    public final Map<Integer, Integer> bitesOffset = new HashMap<>();
    public final Map<Integer, Integer> bitesType = new HashMap<>();
    public final Map<Integer, ResourceLocation> usingConvertsTo = new HashMap<>();

    public static int getFoodHash(Item item, Integer bite) {
        return (item.toString().replace(" ", "") + ":" + (bite != null ? bite : "")).hashCode();
    }

    public static Vec3 getBlockFoodInfo(@NotNull Player player, @NotNull ItemStack stack, Integer bite, FoodProperties _defaultFoodInfo, boolean sliceCalc, int flag) {
        Optional<FoodProperties> defInfo = Optional.ofNullable(_defaultFoodInfo);
        if (!player.isAddedToLevel() || player.tickCount <= 0)
            return defInfo.map(it -> new Vec3(it.nutrition(), it.saturation(), it.eatSeconds())).orElse(new Vec3(0, 0, 1.6F));
        LevelOrgFoodValue data = sliceCalc ? player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE) : new LevelOrgFoodValue();
        int defHash = LevelOrgFoodValue.getFoodHash(stack.getItem(), null);
        Optional<Integer> bites = Optional.ofNullable(data.bites.get(defHash));
        Optional<Integer> bitesOffset = Optional.ofNullable(data.bitesOffset.get(defHash));
        Optional<Integer> bitesType = Optional.ofNullable(data.bitesType.get(defHash));
        Optional<Integer> itemFoodBites = bites;
        bite = bite == null ? (bitesType.isPresent() && bitesType.get() == 1 ? bites.orElse(0) : 0) : bite;
        if (sliceCalc && !data.hash.contains(defHash) && stack.getItem() instanceof BlockItem bi) {
            BlockState blockState = bi.getBlock().defaultBlockState();
            itemFoodBites = blockState.getValues().keySet().stream().map(comparable -> {
                if (comparable instanceof IntegerProperty ip && ip.getName().equals("bites")) {
                    try {
                        Field maxField = IntegerProperty.class.getDeclaredField("max");
                        maxField.setAccessible(true);
                        return (int) maxField.get(ip);
                    } catch (Exception ignored) {

                    }
                }
                return -1;
            }).filter(it -> it > -1).findFirst();
        }

        int finalBites = itemFoodBites.orElse(1);

        var hash0 = LevelOrgFoodValue.getFoodHash(stack.getItem(),bitesType.isPresent() && bitesType.get() == 1 ? finalBites : 0);
        var hunger_direct_def = Optional.ofNullable(data.hunger.get(hash0));
        var saturation_direct_def = Optional.ofNullable(data.saturation.get(hash0));

        var packInfo_def = Optional.ofNullable(data.usingConvertsTo.get(hash0))
                .map(BuiltInRegistries.ITEM::get)
                .map(i -> i.getDefaultInstance().get(DataComponents.FOOD))
                .map(i -> new Vec3(i.nutrition(), i.saturation(), 0));
        var hunger_pack_def = packInfo_def.map(i->(float)i.x);
        var saturation_pack_def = packInfo_def.map(i->(float)i.y);

        AtomicReference<Optional<Float>> eat_seconds = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<Float>> hungerAccRoundErr = new AtomicReference<>(Optional.empty());
        var scope = bitesType.isPresent() && bitesType.get() == 1
                ? IntStream.range(1, bite+1)
                : IntStream.range(bite, Math.max(bite, Math.max(bite, finalBites + bitesOffset.orElse(0))));
        return scope.mapToObj(it -> {
            var tileHunger = 0;
            var tileSaturation = 0F;
            var hashIt = LevelOrgFoodValue.getFoodHash(stack.getItem(), it);
            Optional<ItemStack> packFood = Optional.ofNullable(data.usingConvertsTo.get(hashIt))
                    .map(BuiltInRegistries.ITEM::get)
                    .map(Item::getDefaultInstance);
            Optional<Vec3> packInfo = packFood
                    .map(i->i.get(DataComponents.FOOD))
                    .map(i -> new Vec3(i.nutrition(), i.saturation(), 0));
            var hunger_def = defInfo.map(FoodProperties::nutrition).map(i -> sliceCalc  ? i / (float)finalBites : i);
            var saturation_def = defInfo.map(FoodProperties::saturation).map(i -> sliceCalc  ? i /  (float)finalBites : i);
            var hunger_direct = Optional.ofNullable(data.hunger.get(hashIt));
            var saturation_direct = Optional.ofNullable(data.saturation.get(hashIt));
            var hunger_pack = packInfo.map(i -> (float)i.x);
            var saturation_pack = packInfo.map(i -> (float) i.y);
            var onlyPackInfo = defInfo.isEmpty() && hunger_direct.isEmpty() && (packInfo.isPresent() || packInfo_def.isPresent());
            tileHunger = Math.round(hunger_def
                    .or(() -> hunger_direct)
                    .or(() -> hunger_direct_def)
                    .or(() -> hunger_pack)
                    .or(() -> hunger_pack_def)
                    .orElse(0F)
            );
            tileSaturation = saturation_def
                    .or(() -> saturation_direct)
                    .or(() -> saturation_direct_def)
                    .or(() -> saturation_pack)
                    .or(() -> saturation_pack_def)
                    .orElse(0F);
            var calc = EatFormulaContext.from(player, onlyPackInfo ? packFood.orElse(stack) : stack, new FoodProperties(tileHunger, tileSaturation, defInfo.map(FoodProperties::canAlwaysEat).orElse(false), defInfo.map(FoodProperties::eatSeconds).orElse(1.6F), defInfo.flatMap(FoodProperties::usingConvertsTo), defInfo.map(FoodProperties::effects).orElse(List.of())), flag);
            if (hungerAccRoundErr.get().isEmpty() && calc.isPresent())
                hungerAccRoundErr.set(Optional.of(calc.get().hungerAccRoundErr()));
            if (eat_seconds.get().isEmpty() && calc.isPresent()) eat_seconds.set(Optional.of(calc.get().eat_seconds()));
            return calc.map(c -> new Vec3(c.hunger(), c.saturation(), 0)).orElseGet(() -> new Vec3(0, 0, 0));
        }).reduce(new Vec3(0, 0, 0), Vec3::add).add(new Vec3(hungerAccRoundErr.get().orElse(0F), 0F, eat_seconds.get().orElse(1.6F)));
    }

    public static Optional<Integer> getInfoFinishState(@NotNull Player player, @NotNull ItemStack stack) {
        var data = player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
        var defHash = LevelOrgFoodValue.getFoodHash(stack.getItem(),null);
        if(!data.hash.contains(defHash)) return Optional.empty();
        if (data.bitesType.get(defHash) instanceof Integer type
                && data.bites.get(defHash) instanceof Integer bites
                && data.bitesOffset.get(defHash) instanceof Integer offset
        ) {
            IntStream stream = type == 1 ? IntStream.range(1, bites + 1) : IntStream.range(0, bites + offset);
            return Optional.of(stream.mapToObj(i -> data.hash.contains(LevelOrgFoodValue.getFoodHash(stack.getItem(), i))).anyMatch(i -> !i) ? 1 : 2);
        }
        return stack.get(DataComponents.FOOD) == null ? Optional.empty() : Optional.of(2);
    }

    static final class CustomCodec {

        public static <K, V> Codec<Map<K, V>> mapAsList(Codec<K> keyCodec, Codec<V> valueCodec) {
            Codec<Map.Entry<K, V>> entryCodec = RecordCodecBuilder.create(inst -> inst.group(keyCodec.fieldOf("k").forGetter(Map.Entry::getKey), valueCodec.fieldOf("v").forGetter(Map.Entry::getValue)).apply(inst, Map::entry));

            return Codec.list(entryCodec).xmap(list -> {
                Map<K, V> map = new HashMap<>();
                for (var e : list) map.put(e.getKey(), e.getValue());
                return map;
            }, map -> map.entrySet().stream().toList());
        }

        static final Codec<Map<Integer, Float>> INT_FLOAT_MAP = CustomCodec.mapAsList(Codec.INT, Codec.FLOAT);

        static final Codec<Map<Integer, Integer>> INT_INT_MAP = CustomCodec.mapAsList(Codec.INT, Codec.INT);
        static final Codec<Map<Integer, ResourceLocation>> INT_RL_MAP = CustomCodec.mapAsList(Codec.INT, ResourceLocation.CODEC);
    }


    // ===== Codec（存档） =====
    public static final Codec<LevelOrgFoodValue> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().fieldOf("hash").forGetter(v -> v.hash.stream().toList()),
            CustomCodec.INT_FLOAT_MAP.fieldOf("hunger").forGetter(v -> v.hunger),
            CustomCodec.INT_FLOAT_MAP.fieldOf("saturation").forGetter(v -> v.saturation),
            CustomCodec.INT_INT_MAP.fieldOf("bites").forGetter(v -> v.bites),
            CustomCodec.INT_INT_MAP.fieldOf("bitesOffset").forGetter(v -> v.bitesOffset),
            CustomCodec.INT_INT_MAP.fieldOf("bitesType").forGetter(v -> v.bitesType),
            CustomCodec.INT_RL_MAP.fieldOf("usingConvertsTo").forGetter(v -> v.usingConvertsTo)
    ).apply(i, (a, b, c, d, f, g,h) -> {
        LevelOrgFoodValue v = new LevelOrgFoodValue();
        v.hash.addAll(a);
        v.hunger.putAll(b);
        v.saturation.putAll(c);
        v.bites.putAll(d);
        v.bitesOffset.putAll(f);
        v.bitesType.putAll(g);
        v.usingConvertsTo.putAll(h);
        return v;
    }));

    // ===== StreamCodec（网络同步） =====
    public static final StreamCodec<FriendlyByteBuf, LevelOrgFoodValue> STREAM_CODEC = StreamCodec.of((b, v) -> {
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

        b.writeVarInt(v.bitesType.size());
        v.bitesType.forEach((k, f) -> {
            b.writeVarInt(k);
            b.writeInt(f);
        });

        b.writeVarInt(v.usingConvertsTo.size());
        v.usingConvertsTo.forEach((k, f) -> {
            b.writeVarInt(k);
            ResourceLocation.STREAM_CODEC.encode(b, f);
        });
    }, b -> {
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

        for (int i = b.readVarInt(); i-- > 0; )
            v.bitesType.put(b.readVarInt(), b.readInt());

        for (int i = b.readVarInt(); i-- > 0; )
            v.usingConvertsTo.put(b.readVarInt(), ResourceLocation.STREAM_CODEC.decode(b));

        return v;
    });

}
