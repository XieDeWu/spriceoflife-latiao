package com.xdw.spiceoflifelatiao.attachments;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.MurmurHash3;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class LevelOrgFoodValue {
    // 专门标记数据结构的版本
    public static final int DATA_VERSION = 3;
    public final Set<Integer> hash = new HashSet<>();
    public final Map<Integer, Float> hunger = new HashMap<>();
    public final Map<Integer, Float> saturation = new HashMap<>();
    public final Map<Integer, Integer> bites = new HashMap<>();
    public final Map<Integer, Integer> bitesOffset = new HashMap<>();
    public final Map<Integer, Integer> bitesType = new HashMap<>();
    public final Map<Integer, Map<ResourceLocation, Integer>> usingConvertsTo = new HashMap<>();

    public static int getFoodHash(Item item, Integer bite) {
        String key = (SpiceOfLifeLatiao.VERSION + ":" + item.toString().replace(" ", "") + ":" + (bite != null ? bite : ""));
        return  MurmurHash3.hash32x86(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取一个范围的顺位遍历，含自身
     */
    public static List<Integer> getEnumScope(int min, int max, int init, boolean forward) {
        List<Integer> result = new ArrayList<>();
        int size = max - min;
        if (size <= 0) return result;   // 无效区间，返回空
        int step = forward ? 1 : -1;
        int current = init;
        for (int i = 0; i < size; i++) {
            result.add(current);
            current += step;
            if (current >= max) {
                current = min;
            } else if (current < min) {
                current = max - 1;
            }
        }
        return result;
    }

//    /**
//     * 当物品组仅包含一个条目时，返回该物品的原始营养（不乘数量），用于保持旧版 fallback 链兼容。
//     * 若组大小 != 1 或物品无效，返回 Optional.empty()。
//     */
//    private static Optional<Vec3> getSingleConvertNutrition(Map<ResourceLocation, Integer> converts) {
//        if (converts == null || converts.size() != 1) return Optional.empty();
//        Map.Entry<ResourceLocation, Integer> entry = converts.entrySet().iterator().next();
//        Item item = BuiltInRegistries.ITEM.get(entry.getKey());
//        FoodProperties food = item.getDefaultInstance().get(DataComponents.FOOD);
//        if (food == null) return Optional.empty();
//        return Optional.of(new Vec3(food.nutrition(), food.saturation(), 0));
//    }

    /**
     * 计算分片数与分片范围
     */
    public static Optional<Map.Entry<Integer,List<Integer>>> getAbleBites(@NotNull Player player, @NotNull ItemStack stack, BlockState state, Integer bite
            , boolean sliceCalc) {
        // 玩家未加载,调整食物信息无意义
        if (!player.isAddedToLevel() || player.tickCount <= 0) return Optional.empty();
        // 计算物品分片逻辑以及范围
        LevelOrgFoodValue data = sliceCalc ? player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE) :
                new LevelOrgFoodValue();
        int defHash = LevelOrgFoodValue.getFoodHash(stack.getItem(), null);
        Optional<Integer> bites = Optional.ofNullable(data.bites.get(defHash));
        Optional<Integer> bitesOffset = Optional.ofNullable(data.bitesOffset.get(defHash));
        Optional<Integer> bitesType = Optional.ofNullable(data.bitesType.get(defHash));
        Optional<Integer> itemFoodBites = bites;
        bite = bite == null ? (bitesType.isPresent() && bitesType.get() == 1 ? bites.orElse(0) : 0) : bite;
        if (sliceCalc && !data.hash.contains(defHash) && stack.getItem() instanceof BlockItem bi) {
            BlockState blockState = state != null ? state : bi.getBlock().defaultBlockState();
            itemFoodBites = blockState.getValues().keySet().stream().map(comparable -> {
                if (comparable instanceof IntegerProperty ip && (ip.getName().equals("bites") || ip.getName().equals("servings"))) {
                    try {
                        Field maxField = IntegerProperty.class.getDeclaredField("max");
                        maxField.setAccessible(true);
                        return (int) maxField.get(ip);
                    } catch (Exception ignored) {}
                }
                return -1;
            }).filter(it -> it > -1).findFirst();
        }
        int finalBites = itemFoodBites.orElse(1);

        // 计算bite顺位表
        var ableBite = bitesType.isPresent() && bitesType.get() == 1
                ? getEnumScope(1, bite + 1, bite, false)
                : getEnumScope(bite, Math.max(bite, finalBites + bitesOffset.orElse(0)), bite, true);
        return Optional.of(Map.entry(finalBites,ableBite));
    }

    /**
     * 获取方块食物信息的主方法
     */
    public static Vec3 getBlockFoodInfo(@NotNull Player player, @NotNull ItemStack stack,BlockState state, Integer bite,
                                        FoodProperties _defaultFoodInfo, boolean sliceCalc, int flag) {
        // 玩家未加载,调整食物信息无意义
        Optional<FoodProperties> defInfo = Optional.ofNullable(_defaultFoodInfo);
        if (!player.isAddedToLevel() || player.tickCount <= 0)
            return defInfo.map(it -> new Vec3(it.nutrition(), it.saturation(), it.eatSeconds()))
                    .orElse(new Vec3(0, 0, 1.6F));

        // 计算物品分片逻辑以及范围
        LevelOrgFoodValue data = sliceCalc ? player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE) : new LevelOrgFoodValue();
        var calcBites = getAbleBites(player, stack, state, bite, sliceCalc).orElse(Map.entry(0,List.of()));
        var ableBite = calcBites.getValue();
        var finalBites = calcBites.getKey();

        // 全bite展开 全bite范围补位 按ableBite范围选择性汇总
        var allBiteHash = IntStream.range(0,16)
                .mapToObj(it -> LevelOrgFoodValue.getFoodHash(stack.getItem(), it))
                .toList();
        var ableBiteHash = ableBite.stream()
                .map(it -> LevelOrgFoodValue.getFoodHash(stack.getItem(), it))
                .toList();

        // 按组(bite,convert)扁平化展开,convert至少为自身
        var flatBiteInfo = allBiteHash.stream().flatMap(hash -> data.usingConvertsTo
                .getOrDefault(hash, Map.of(BuiltInRegistries.ITEM.getKey(stack.getItem()), 1))
                .entrySet().stream()
                .map(it -> {
                    ItemStack cov = BuiltInRegistries.ITEM.get(it.getKey()).getDefaultInstance();
                    cov.setCount(it.getValue());
                    return Map.entry(hash, cov);
                })).toList();

        // 获取bite已记录foodInfo
        var flat2BiteInfo = flatBiteInfo.stream().map(it->{
            var hunger_direct = Optional.ofNullable(data.hunger.get(it.getKey()));
            var saturation_direct = Optional.ofNullable(data.saturation.get(it.getKey()));
            // defInfo作为direct的补充信息
            var direct = hunger_direct.flatMap(k->saturation_direct.map(v->new FoodProperties(
                    Math.round(k),
                    v,
                    defInfo.map(FoodProperties::canAlwaysEat).orElse(false),
                    defInfo.map(FoodProperties::eatSeconds).orElse(1.6F),
                    defInfo.flatMap(FoodProperties::usingConvertsTo),
                    defInfo.map(FoodProperties::effects).orElse(List.of())
            )));
            var convert = Optional.ofNullable(it.getValue().get(DataComponents.FOOD));
            return Map.entry(it,direct.or(()->convert));
        }).toList();

        // 按 bite 保序分组,成组邻居补位
        var biteGroups = flat2BiteInfo.stream().collect((Supplier<ArrayList<List<Map.Entry<Map.Entry<Integer,
                ItemStack>, Optional<FoodProperties>>>>>) ArrayList::new, (list, e) -> {
            if (list.isEmpty() || !Objects.equals(e.getKey().getKey(), list.getLast().getFirst().getKey().getKey()))
                list.add(new ArrayList<>());
            list.getLast().add(e);
        }, (l, r) -> {throw new UnsupportedOperationException();});
        var extendBiteInfo = IntStream.range(0, biteGroups.size()).mapToObj(idx -> {
            var group = biteGroups.get(idx);
            if (group.stream().anyMatch(e -> e.getValue().isPresent())) return group;
            // 找第一个有信息的邻居组
            var neighbor = IntStream.range(1, biteGroups.size())
                    .mapToObj(d -> biteGroups.get((idx + d) % biteGroups.size()))
                    .filter(g -> g.stream().anyMatch(e -> e.getValue().isPresent()))
                    .findFirst();
            return neighbor.map(n -> n.stream().map(ne -> Map.entry(Map.entry(group.getFirst().getKey().getKey(),
                    ne.getKey().getValue()), ne.getValue())).toList()).orElse(group);
        }).flatMap(Collection::stream).toList();

        // 若外部为此物品输入食物信息,优先使用外部定义
        List<Map.Entry<Map.Entry<Integer, ItemStack>, Optional<FoodProperties>>> redirectInfo =
                extendBiteInfo.stream().map(it -> Map.entry(it.getKey(),
                        defInfo.map(def -> new FoodProperties(Math.round(sliceCalc ?
                                def.nutrition() / (float) finalBites : def.nutrition()), sliceCalc ?
                                def.saturation() / (float) finalBites : def.saturation(), def.canAlwaysEat(),
                                def.eatSeconds(), def.usingConvertsTo(), def.effects())).or(it::getValue))).toList();

        // 套公式
        var calcOutput = redirectInfo.stream()
                .filter(entry -> ableBiteHash.stream().anyMatch(i-> Objects.equals(i, entry.getKey().getKey())))
                .map(it -> Map.entry(
                it.getKey().getValue().getCount(),
                it.getValue().flatMap(info -> EatFormulaContext.from(player, it.getKey().getValue(), info, flag))
        )).toList();

        // 汇总
        int sumCount = calcOutput.stream().map(Map.Entry::getKey).reduce(0, Integer::sum);
        float sumHunger = calcOutput.stream().flatMap(it ->
                it.getValue().stream().map(EatFormulaContext::hunger).map(j -> j * it.getKey())
        ).reduce(0F, Float::sum);
        float sumSaturation = calcOutput.stream().flatMap(it ->
                it.getValue().stream().map(EatFormulaContext::saturation).map(j -> j * it.getKey())
        ).reduce(0F, Float::sum);
        float sumSecond = calcOutput.stream().flatMap(it ->
                it.getValue().stream().map(EatFormulaContext::eat_seconds).map(j -> j * it.getKey())
        ).reduce(0F, Float::sum);
        float roundErr = calcOutput.stream().flatMap(i -> i.getValue()
                .map(EatFormulaContext::hungerAccRoundErr).stream()
        ).findFirst().orElse(0F);
        return new Vec3(sumHunger + roundErr, sumSaturation, sumSecond / Math.max(1, sumCount));
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
        static final Codec<Map<ResourceLocation, Integer>> RL_INT_MAP = mapAsList(ResourceLocation.CODEC, Codec.INT);
        static final Codec<Map<Integer, Map<ResourceLocation, Integer>>> INT_RL_INT_MAP = mapAsList(Codec.INT, RL_INT_MAP);
    }


    // ===== Codec（存档） =====
    public static final Codec<LevelOrgFoodValue> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("dataVersion", 0).forGetter(v -> DATA_VERSION),
            Codec.INT.listOf().fieldOf("hash").forGetter(v -> v.hash.stream().toList()),
            CustomCodec.INT_FLOAT_MAP.fieldOf("hunger").forGetter(v -> v.hunger),
            CustomCodec.INT_FLOAT_MAP.fieldOf("saturation").forGetter(v -> v.saturation),
            CustomCodec.INT_INT_MAP.fieldOf("bites").forGetter(v -> v.bites),
            CustomCodec.INT_INT_MAP.fieldOf("bitesOffset").forGetter(v -> v.bitesOffset),
            CustomCodec.INT_INT_MAP.fieldOf("bitesType").forGetter(v -> v.bitesType),
            CustomCodec.INT_RL_INT_MAP.fieldOf("usingConvertsTo").forGetter(v -> v.usingConvertsTo)
    ).apply(i, (version,a, b, c, d, f, g,h) -> {
        if (version != DATA_VERSION) {
            // 版本不匹配 → 直接返回全新空对象，旧数据全部丢弃
            LogUtils.getLogger().warn("The data storage format of SpiceOfLifeLatiao-v{} has been changed. The old world block food records have been emptied.",SpiceOfLifeLatiao.VERSION);
            return new LevelOrgFoodValue();
        }
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
        v.usingConvertsTo.forEach((k, map) -> {
            b.writeVarInt(k);
            b.writeVarInt(map.size());
            map.forEach((rl, count) -> {
                ResourceLocation.STREAM_CODEC.encode(b, rl);
                b.writeVarInt(count);
            });
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

        for (int i = b.readVarInt(); i-- > 0; ) {
            int key = b.readVarInt();
            int size = b.readVarInt();
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (int j = 0; j < size; j++) {
                ResourceLocation rl = ResourceLocation.STREAM_CODEC.decode(b);
                int count = b.readVarInt();
                map.put(rl, count);
            }
            v.usingConvertsTo.put(key, map);
        }

        return v;
    });

}
