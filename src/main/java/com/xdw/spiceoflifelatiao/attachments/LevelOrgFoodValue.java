package com.xdw.spiceoflifelatiao.attachments;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.cached.LevelCalcCached;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.MurmurHash3;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
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
import java.util.concurrent.atomic.AtomicReference;

public final class LevelOrgFoodValue {
    public static final int DATA_VERSION = 5;
    public final Set<Integer> hash = new ObjectOpenHashSet<>(1024);
    public final Map<Integer, Float> hunger = new HashMap<>(1024);
    public final Map<Integer, Float> saturation = new HashMap<>(1024);
    public final Map<Integer, Integer> bites = new HashMap<>(1024);
    public final Map<Integer, Integer> bitesOffset = new HashMap<>(1024);
    public final Map<Integer, Integer> bitesType = new HashMap<>(1024);
    public final Map<Integer, Map<ResourceLocation, Integer>> usingConvertsTo = new HashMap<>(1024);

    //食物单片舍入补正
    public static final HashMap<Integer, Map.Entry<AtomicReference<Float>, AtomicReference<Float>>>
            divRoundErr = HashMap.newHashMap(16);

    // ★ 结果缓存
    private static final Map<Integer, Vec3> RESULT_CACHE = new HashMap<>(256);
    private static long lastClearTime = 0;
    private static int hashResultCacheKey(int playerId, Item item, String blockTagId,
                                          int bite, int flag, boolean sliceCalc, long timeSlice) {
        int h = playerId;
        h = 31 * h + System.identityHashCode(item);
        h = 31 * h + (blockTagId != null ? blockTagId.hashCode() : 0);
        h = 31 * h + bite;
        h = 31 * h + flag;
        h = 31 * h + (sliceCalc ? 1 : 0);
        h = 31 * h + Long.hashCode(timeSlice);
        return h;
    }

    // ★ 带内部缓存的 getFoodHash，缓存整个 (item, id) 组的所有哈希
    private static final Map<Integer,String> cachedRegNames = new HashMap<>(64);
    public static String getCachedRegName(Item item) {
        var id = System.identityHashCode(item);
        var name = cachedRegNames.get(id);
        if (name == null) {
            name = item.toString().replace(" ", "");
            cachedRegNames.put(id, name);
        }
        return name;
    }
    private record HashCacheEntry(int nullHash, int[] biteHashes) {}
    private static final Map<Integer, HashCacheEntry> FOOD_HASH_CACHE = new HashMap<>(512);
    public static int getFoodHash(Item item, Integer bite, String id) {
        String safeId = id != null ? id : "";
        var regName = getCachedRegName(item);
        int key = regName.hashCode() ^ safeId.hashCode();
        HashCacheEntry entry = FOOD_HASH_CACHE.get(key);
        if (entry == null) {
            // 批量计算 null 和 0~15 的哈希
            String base = SpiceOfLifeLatiao.VERSION + ":" + regName + ":";
            int nullHash = MurmurHash3.hash32x86((base + ":" + safeId).getBytes(StandardCharsets.UTF_8));
            int[] hashes = new int[16];
            for (int b = 0; b < 16; b++) {
                hashes[b] = MurmurHash3.hash32x86((base + b + ":" + safeId).getBytes(StandardCharsets.UTF_8));
            }
            entry = new HashCacheEntry(nullHash, hashes);
            FOOD_HASH_CACHE.put(key, entry);
        }
        return bite == null ? entry.nullHash : entry.biteHashes[bite];
    }

    /** 返回 int[]，避免 List 装箱 */
    public static int[] getEnumScope(int min, int max, int init, boolean forward) {
        int size = max - min;
        if (size <= 0) return new int[0];
        int[] result = new int[size];
        int step = forward ? 1 : -1;
        int current = init;
        for (int i = 0; i < size; i++) {
            result[i] = current;
            current += step;
            if (current >= max) current = min;
            else if (current < min) current = max - 1;
        }
        return result;
    }

    public static Optional<Map.Entry<Integer, int[]>> getAbleBites(@NotNull Player player, @NotNull ItemStack stack,
                                                                   BlockState state, Integer bite,
                                                                   String blockTagId, boolean sliceCalc) {
        if (!player.isAddedToLevel() || player.tickCount <= 0) return Optional.empty();
        LevelOrgFoodValue data = sliceCalc ? player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE)
                : new LevelOrgFoodValue();
        int defHash = getFoodHash(stack.getItem(), null, blockTagId);
        Integer bitesVal = data.bites.get(defHash);
        Integer offsetVal = data.bitesOffset.get(defHash);
        Integer typeVal = data.bitesType.get(defHash);
        int resolvedBite = (bite != null) ? bite : (typeVal != null && typeVal == 1 ? (bitesVal != null ? bitesVal : 0) : 0);

        int finalBites = 1;
        if (bitesVal != null) finalBites = bitesVal;
        if (sliceCalc && !data.hash.contains(defHash) && stack.getItem() instanceof BlockItem bi) {
            BlockState blockState = state != null ? state : bi.getBlock().defaultBlockState();
            for (var entry : blockState.getValues().entrySet()) {
                if (entry.getKey() instanceof IntegerProperty ip &&
                        (ip.getName().equals("bites") || ip.getName().equals("servings"))) {
                    try {
                        Field maxField = IntegerProperty.class.getDeclaredField("max");
                        maxField.setAccessible(true);
                        finalBites = (int) maxField.get(ip);
                        break;
                    } catch (Exception ignored) {}
                }
            }
        }

        int maxBite = Math.max(resolvedBite, finalBites + (offsetVal != null ? offsetVal : 0));
        int[] ableBite = typeVal != null && typeVal == 1
                ? getEnumScope(1, resolvedBite + 1, resolvedBite, false)
                : getEnumScope(resolvedBite, maxBite, resolvedBite, true);
        return Optional.of(Map.entry(finalBites, ableBite));
    }


    public static Vec3 getBlockFoodInfo(@NotNull Player player, @NotNull ItemStack stack, BlockState state,
                                        Integer bite, String blockTagId, FoodProperties def,
                                        boolean sliceCalc, int flag) {
        if (!player.isAddedToLevel() || player.tickCount <= 0) {
            if (def != null) return new Vec3(def.nutrition(), def.saturation(), def.eatSeconds());
            return new Vec3(0, 0, 1.6F);
        }

        if (Math.abs(LevelCalcCached.gameTime - lastClearTime) > 600) {
            lastClearTime = LevelCalcCached.gameTime;
            RESULT_CACHE.clear();
        }

        long timeSlice = LevelCalcCached.gameTime / 20;
        int key = hashResultCacheKey(
                player.getId(),
                stack.getItem(),
                blockTagId,
                bite != null ? bite : -1,
                flag,
                sliceCalc,
                timeSlice
        );
        Vec3 cached = RESULT_CACHE.get(key);
        if (cached != null) return cached;

        LevelOrgFoodValue data = sliceCalc ? player.level().getData(ModAttachments.LEVEL_ORG_FOOD_VALUE) : new LevelOrgFoodValue();
        var calcBites = getAbleBites(player, stack, state, bite, blockTagId, sliceCalc)
                .orElse(Map.entry(0, new int[0]));
        int[] ableBite = calcBites.getValue();
        int finalBites = calcBites.getKey();
        if (ableBite.length == 0 || finalBites <= 0) {
            Vec3 result = new Vec3(0, 0, 1.6F);
            RESULT_CACHE.put(key, result);
            return result;
        }

        Item item = stack.getItem();
        int[] allHash = new int[16];
        for (int b = 0; b < 16; b++) {
            allHash[b] = getFoodHash(item, b, blockTagId);
        }

        boolean canAlwaysEat = def != null && def.canAlwaysEat();
        float eatSeconds = def != null ? def.eatSeconds() : 1.6F;
        List<FoodProperties.PossibleEffect> effects = def != null ? def.effects() : List.of();
        Optional<ItemStack> defConvert = def != null ? def.usingConvertsTo() : Optional.empty();

        record BiteData(Float hunger, Float saturation, Map<ResourceLocation, Integer> converts) {}
        BiteData[] biteDataCache = new BiteData[16];
        for (int b = 0; b < 16; b++) {
            int hash = allHash[b];
            biteDataCache[b] = new BiteData(
                    data.hunger.get(hash),
                    data.saturation.get(hash),
                    data.usingConvertsTo.get(hash)
            );
        }

        class BiteEntry {
            ItemStack convStack;
            FoodProperties food;
        }

        // 预先记录每个组是否有有效食物
        boolean[] groupHasFood = new boolean[16];
        List<List<BiteEntry>> biteGroups = new ArrayList<>(16);
        for (int b = 0; b < 16; b++) {
            BiteData bd = biteDataCache[b];
            FoodProperties directFood = (bd.hunger != null && bd.saturation != null)
                    ? new FoodProperties(Math.round(bd.hunger), bd.saturation, canAlwaysEat, eatSeconds, defConvert, effects)
                    : null;

            List<BiteEntry> group = new ArrayList<>(1);
            if (bd.converts == null || bd.converts.isEmpty()) {
                BiteEntry entry = new BiteEntry();
                entry.food = directFood;
                group.add(entry);
                groupHasFood[b] = entry.food != null;
            } else {
                for (var convEntry : bd.converts.entrySet()) {
                    ItemStack conv = BuiltInRegistries.ITEM.get(convEntry.getKey()).getDefaultInstance();
                    conv.setCount(convEntry.getValue());
                    BiteEntry entry = new BiteEntry();
                    entry.convStack = conv;
                    if (directFood != null) {
                        entry.food = directFood;
                    } else {
                        entry.food = conv.get(DataComponents.FOOD);
                    }
                    group.add(entry);
                    // 任意一个条目有食物即可标记
                    if (entry.food != null) groupHasFood[b] = true;
                }
            }
            biteGroups.add(group);
        }

        // ★ 邻居补位，直接使用 groupHasFood 布尔数组
        for (int b = 0; b < 16; b++) {
            if (!groupHasFood[b]) {
                int neighborIdx = -1;
                for (int d = 1; d < 16; d++) {
                    int idx = (b + d) % 16;
                    if (groupHasFood[idx]) {
                        neighborIdx = idx;
                        break;
                    }
                }
                if (neighborIdx != -1) {
                    List<BiteEntry> neighbor = biteGroups.get(neighborIdx);
                    List<BiteEntry> newGroup = new ArrayList<>(neighbor.size());
                    for (BiteEntry ne : neighbor) {
                        BiteEntry newEntry = new BiteEntry();
                        newEntry.convStack = ne.convStack;
                        newEntry.food = ne.food;
                        newGroup.add(newEntry);
                    }
                    biteGroups.set(b, newGroup);
                    groupHasFood[b] = true; // 补位后标记为有食物
                }
            }
        }

        var errKV = divRoundErr.computeIfAbsent(player.getId(),
                k -> Map.entry(new AtomicReference<>(0f), new AtomicReference<>(0f)));
        float[] error = new float[]{ errKV.getKey().get() };

        int sumCount = 0;
        float sumHunger = 0f, sumSaturation = 0f, sumSecond = 0f;
        float hungerRoundErr = 0f;

        FoodProperties selfFood = stack.get(DataComponents.FOOD);

        for (int b : ableBite) {
            List<BiteEntry> group = biteGroups.get(b);
            for (BiteEntry entry : group) {
                final FoodProperties finalFood;
                if (def != null) {
                    finalFood = applyRound(def, sliceCalc, finalBites, error);
                } else if (entry.food != null) {
                    finalFood = entry.food;
                } else {
                    FoodProperties fallbackFood = entry.convStack != null
                                    ? entry.convStack.get(DataComponents.FOOD)
                                    : selfFood;
                    if (fallbackFood != null) {
                        finalFood = applyRound(fallbackFood, sliceCalc, finalBites, error);
                    } else {
                        continue;
                    }
                }

                ItemStack convStack = entry.convStack != null ? entry.convStack : stack;
                var optResult = EatFormulaContext.from(player, convStack, finalFood, flag);
                if (optResult.isPresent()) {
                    var res = optResult.get();
                    int count = convStack.getCount();
                    sumCount += count;
                    sumHunger += res.hunger() * count;
                    sumSaturation += res.saturation() * count;
                    sumSecond += res.eat_seconds() * count;
                    if (hungerRoundErr == 0f) hungerRoundErr = res.hungerAccRoundErr();
                }
            }
        }

        errKV.getValue().set(error[0]);
        Vec3 result = new Vec3(sumHunger + hungerRoundErr, sumSaturation, sumSecond / Math.max(1, sumCount));
        RESULT_CACHE.put(key, result);
        return result;
    }

    private static FoodProperties applyRound(FoodProperties base, boolean sliceCalc, int finalBites, float[] roundErr) {
        float target = sliceCalc
                ? (base.nutrition() + roundErr[0]) / (float) finalBites
                : (base.nutrition() + roundErr[0]);
        int real = Math.round(target);
        roundErr[0] += target - real;
        return new FoodProperties(
                real,
                sliceCalc ? base.saturation() / (float) finalBites : base.saturation(),
                base.canAlwaysEat(),
                base.eatSeconds(),
                base.usingConvertsTo(),
                base.effects()
        );
    }

    // ==================== Codec / StreamCodec 保持不变 ====================
    static final class CustomCodec {
        public static <K, V> Codec<Map<K, V>> mapAsList(Codec<K> keyCodec, Codec<V> valueCodec) {
            Codec<Map.Entry<K, V>> entryCodec = RecordCodecBuilder.create(inst -> inst.group(
                    keyCodec.fieldOf("k").forGetter(Map.Entry::getKey),
                    valueCodec.fieldOf("v").forGetter(Map.Entry::getValue)
            ).apply(inst, Map::entry));
            return Codec.list(entryCodec).xmap(list -> {
                Map<K, V> map = new HashMap<>();
                for (var e : list) map.put(e.getKey(), e.getValue());
                return map;
            }, map -> map.entrySet().stream().toList());
        }
        static final Codec<Map<Integer, Float>> INT_FLOAT_MAP = mapAsList(Codec.INT, Codec.FLOAT);
        static final Codec<Map<Integer, Integer>> INT_INT_MAP = mapAsList(Codec.INT, Codec.INT);
        static final Codec<Map<Integer, ResourceLocation>> INT_RL_MAP = mapAsList(Codec.INT, ResourceLocation.CODEC);
        static final Codec<Map<ResourceLocation, Integer>> RL_INT_MAP = mapAsList(ResourceLocation.CODEC, Codec.INT);
        static final Codec<Map<Integer, Map<ResourceLocation, Integer>>> INT_RL_INT_MAP = mapAsList(Codec.INT, RL_INT_MAP);
    }

    public static final Codec<LevelOrgFoodValue> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("dataVersion", 0).forGetter(v -> DATA_VERSION),
            Codec.INT.listOf().fieldOf("hash").forGetter(v -> v.hash.stream().toList()),
            CustomCodec.INT_FLOAT_MAP.fieldOf("hunger").forGetter(v -> v.hunger),
            CustomCodec.INT_FLOAT_MAP.fieldOf("saturation").forGetter(v -> v.saturation),
            CustomCodec.INT_INT_MAP.fieldOf("bites").forGetter(v -> v.bites),
            CustomCodec.INT_INT_MAP.fieldOf("bitesOffset").forGetter(v -> v.bitesOffset),
            CustomCodec.INT_INT_MAP.fieldOf("bitesType").forGetter(v -> v.bitesType),
            CustomCodec.INT_RL_INT_MAP.fieldOf("usingConvertsTo").forGetter(v -> v.usingConvertsTo)
    ).apply(i, (version, a, b, c, d, f, g, h) -> {
        if (version != DATA_VERSION) {
            LogUtils.getLogger().warn("The data storage format of SpiceOfLifeLatiao-v{} has been changed. The old world block food records have been emptied.", SpiceOfLifeLatiao.VERSION);
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

    public static final StreamCodec<FriendlyByteBuf, LevelOrgFoodValue> STREAM_CODEC = StreamCodec.of((b, v) -> {
        b.writeVarInt(v.hash.size());
        v.hash.forEach(b::writeVarInt);
        b.writeVarInt(v.hunger.size());
        v.hunger.forEach((k, val) -> { b.writeVarInt(k); b.writeFloat(val); });
        b.writeVarInt(v.saturation.size());
        v.saturation.forEach((k, val) -> { b.writeVarInt(k); b.writeFloat(val); });
        b.writeVarInt(v.bites.size());
        v.bites.forEach((k, val) -> { b.writeVarInt(k); b.writeInt(val); });
        b.writeVarInt(v.bitesOffset.size());
        v.bitesOffset.forEach((k, val) -> { b.writeVarInt(k); b.writeInt(val); });
        b.writeVarInt(v.bitesType.size());
        v.bitesType.forEach((k, val) -> { b.writeVarInt(k); b.writeInt(val); });
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
        for (int i = b.readVarInt(); i-- > 0; ) v.hash.add(b.readVarInt());
        for (int i = b.readVarInt(); i-- > 0; ) v.hunger.put(b.readVarInt(), b.readFloat());
        for (int i = b.readVarInt(); i-- > 0; ) v.saturation.put(b.readVarInt(), b.readFloat());
        for (int i = b.readVarInt(); i-- > 0; ) v.bites.put(b.readVarInt(), b.readInt());
        for (int i = b.readVarInt(); i-- > 0; ) v.bitesOffset.put(b.readVarInt(), b.readInt());
        for (int i = b.readVarInt(); i-- > 0; ) v.bitesType.put(b.readVarInt(), b.readInt());
        for (int i = b.readVarInt(); i-- > 0; ) {
            int key = b.readVarInt();
            int size = b.readVarInt();
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (int j = 0; j < size; j++)
                map.put(ResourceLocation.STREAM_CODEC.decode(b), b.readVarInt());
            v.usingConvertsTo.put(key, map);
        }
        return v;
    });
}