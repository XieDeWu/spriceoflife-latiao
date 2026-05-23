package com.xdw.spiceoflifelatiao.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Human editable manual edible-item database.
 *
 * Location: <server root>/config/spiceoflifelatiao/edible_items.json
 *
 * The old Level attachment is still used for automatic discovery, but this file is the authoritative
 * place for administrator-created edible items and survives world reloads / server restarts.
 */
public final class ManualFoodConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(SpiceOfLifeLatiao.MODID)
            .resolve("edible_items.json");

    private static Store store = new Store();
    private static boolean loaded = false;

    private ManualFoodConfig() {}

    public static Path path() {
        return CONFIG_PATH;
    }

    public static synchronized void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) {
                store = Store.defaults();
                save();
                loaded = true;
                return;
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                Store parsed = GSON.fromJson(reader, Store.class);
                store = parsed == null ? Store.defaults() : parsed.sanitized();
            }
            loaded = true;
        } catch (IOException | JsonSyntaxException e) {
            SpiceOfLifeLatiao.LOGGER.error("Failed to load manual food config from {}", CONFIG_PATH, e);
            store = Store.defaults();
            loaded = true;
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(store.sanitized(), writer);
            }
        } catch (IOException e) {
            SpiceOfLifeLatiao.LOGGER.error("Failed to save manual food config to {}", CONFIG_PATH, e);
        }
    }

    private static synchronized Store getStore() {
        if (!loaded) load();
        return store;
    }

    public static synchronized Map<String, Entry> globalItems() {
        return Collections.unmodifiableMap(new TreeMap<>(getStore().global));
    }

    public static synchronized Map<String, Entry> worldItems(ResourceLocation dimension) {
        return Collections.unmodifiableMap(new TreeMap<>(getStore().worlds.getOrDefault(dimension.toString(), new TreeMap<>())));
    }

    public static synchronized Map<String, Map<String, Entry>> allWorldItems() {
        Map<String, Map<String, Entry>> out = new TreeMap<>();
        getStore().worlds.forEach((world, items) -> out.put(world, Collections.unmodifiableMap(new TreeMap<>(items))));
        return Collections.unmodifiableMap(out);
    }


    /**
     * True when an item has any enabled manual entry, including dimension-scoped entries.
     * Useful for client-only places where no Level is available, such as use animation.
     */
    public static synchronized boolean hasAnyEnabledEntry(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return false;
        String key = itemId.toString();
        Store s = getStore();
        Entry globalEntry = s.global.get(key);
        if (globalEntry != null && globalEntry.enabled) return true;
        return s.worlds.values().stream().anyMatch(items -> {
            Entry entry = items.get(key);
            return entry != null && entry.enabled;
        });
    }

    /**
     * Default item components are global, not dimension-scoped. Global entries are preferred.
     * World entries are accepted as a compatibility fallback for entries created with
     * /sol_latiao food add current, but only when the item has no global entry.
     */
    public static synchronized Map<String, Entry> startupComponentEntries() {
        TreeMap<String, Entry> out = new TreeMap<>();
        getStore().global.forEach((id, entry) -> {
            if (entry != null && entry.enabled) out.put(id, entry.copy().sanitized());
        });
        getStore().worlds.forEach((dimension, items) -> {
            if (items == null) return;
            items.forEach((id, entry) -> {
                if (entry != null && entry.enabled) out.putIfAbsent(id, entry.copy().sanitized());
            });
        });
        return Collections.unmodifiableMap(out);
    }

    public static synchronized Optional<Entry> getEntry(@Nullable Level level, Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return Optional.empty();
        return getEntry(level, itemId);
    }

    public static synchronized Optional<Entry> getEntry(@Nullable Level level, ResourceLocation itemId) {
        Store s = getStore();
        if (level != null) {
            Map<String, Entry> world = s.worlds.get(level.dimension().location().toString());
            if (world != null) {
                Entry entry = world.get(itemId.toString());
                if (entry != null && entry.enabled) return Optional.of(entry.copy());
            }
        }
        Entry entry = s.global.get(itemId.toString());
        return entry != null && entry.enabled ? Optional.of(entry.copy()) : Optional.empty();
    }

    public static Optional<FoodProperties> getFoodProperties(@Nullable Level level, ItemStack stack, @Nullable FoodProperties fallback) {
        return getEntry(level, stack.getItem()).flatMap(entry -> entry.toFoodProperties(fallback));
    }

    public static synchronized void putGlobal(ResourceLocation itemId, Entry entry) {
        Store s = getStore();
        s.global.put(itemId.toString(), entry.sanitized());
        save();
    }

    public static synchronized void putWorld(ResourceLocation dimension, ResourceLocation itemId, Entry entry) {
        Store s = getStore();
        s.worlds.computeIfAbsent(dimension.toString(), ignored -> new TreeMap<>()).put(itemId.toString(), entry.sanitized());
        save();
    }

    public static synchronized boolean removeGlobal(ResourceLocation itemId) {
        boolean removed = getStore().global.remove(itemId.toString()) != null;
        if (removed) save();
        return removed;
    }

    public static synchronized boolean removeWorld(ResourceLocation dimension, ResourceLocation itemId) {
        Map<String, Entry> world = getStore().worlds.get(dimension.toString());
        if (world == null) return false;
        boolean removed = world.remove(itemId.toString()) != null;
        if (world.isEmpty()) getStore().worlds.remove(dimension.toString());
        if (removed) save();
        return removed;
    }

    public static synchronized Entry getOrCreate(@Nullable ResourceLocation dimension, ResourceLocation itemId) {
        Store s = getStore();
        Entry entry;
        if (dimension == null) {
            entry = s.global.computeIfAbsent(itemId.toString(), ignored -> new Entry());
        } else {
            entry = s.worlds.computeIfAbsent(dimension.toString(), ignored -> new TreeMap<>())
                    .computeIfAbsent(itemId.toString(), ignored -> new Entry());
        }
        entry.enabled = true;
        return entry;
    }

    public static boolean isValidItem(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.containsKey(itemId);
    }

    public static boolean isValidEffect(ResourceLocation effectId) {
        return BuiltInRegistries.MOB_EFFECT.containsKey(effectId);
    }

    public static Optional<Item> item(ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.get(id));
    }

    public static String describeEntry(ResourceLocation itemId, Entry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(itemId).append(" hunger=").append(entry.nutrition == null ? "default" : entry.nutrition);
        sb.append(" saturation=").append(entry.saturation == null ? "default" : entry.saturation);
        sb.append(" eatSeconds=").append(entry.eatSeconds == null ? "default" : entry.eatSeconds);
        sb.append(" alwaysEat=").append(entry.canAlwaysEat == null ? "default" : entry.canAlwaysEat);
        if (entry.usingConvertsTo != null && !entry.usingConvertsTo.isBlank()) sb.append(" convert=").append(entry.usingConvertsTo);
        if (entry.bites != null) sb.append(" bites=").append(entry.bites);
        if (entry.bitesType != null) sb.append(" type=").append(entry.bitesType == 1 ? "servings" : "bites");
        if (entry.bitesOffset != null) sb.append(" offset=").append(entry.bitesOffset);
        if (entry.effects != null && !entry.effects.isEmpty()) sb.append(" effects=").append(entry.effects.size());
        return sb.toString();
    }

    public static final class Store {
        public int format = 1;
        public String comment = "Manual edible items. Global entries apply to every loaded world; worlds entries override global per dimension.";
        public Map<String, Entry> global = new TreeMap<>();
        public Map<String, Map<String, Entry>> worlds = new TreeMap<>();

        public static Store defaults() {
            return new Store().sanitized();
        }

        public Store sanitized() {
            if (format <= 0) format = 1;
            if (global == null) global = new TreeMap<>();
            if (worlds == null) worlds = new TreeMap<>();
            global.replaceAll((k, v) -> v == null ? new Entry() : v.sanitized());
            worlds.replaceAll((world, items) -> {
                Map<String, Entry> out = new TreeMap<>();
                if (items != null) items.forEach((item, entry) -> out.put(item, entry == null ? new Entry() : entry.sanitized()));
                return out;
            });
            return this;
        }
    }

    public static final class Entry {
        public boolean enabled = true;
        public Integer nutrition = null;
        public Float saturation = null;
        public Float eatSeconds = null;
        public Boolean canAlwaysEat = null;
        public String usingConvertsTo = null;
        public Integer bites = null;
        /** 0 = bites decreasing from 0, 1 = servings accumulating from 1. */
        public Integer bitesType = null;
        public Integer bitesOffset = null;
        public List<EffectEntry> effects = new ArrayList<>();

        public Entry sanitized() {
            if (saturation != null && (!Float.isFinite(saturation) || saturation < 0)) saturation = 0F;
            if (eatSeconds != null && (!Float.isFinite(eatSeconds) || eatSeconds <= 0)) eatSeconds = 1.6F;
            if (nutrition != null && nutrition < 0) nutrition = 0;
            if (bites != null && bites < 1) bites = 1;
            if (bitesType != null && bitesType != 0 && bitesType != 1) bitesType = 0;
            if (bitesOffset != null && bitesOffset < 0) bitesOffset = 0;
            if (effects == null) effects = new ArrayList<>();
            effects.replaceAll(e -> e == null ? new EffectEntry() : e.sanitized());
            return this;
        }

        public Entry copy() {
            Entry copy = new Entry();
            copy.enabled = enabled;
            copy.nutrition = nutrition;
            copy.saturation = saturation;
            copy.eatSeconds = eatSeconds;
            copy.canAlwaysEat = canAlwaysEat;
            copy.usingConvertsTo = usingConvertsTo;
            copy.bites = bites;
            copy.bitesType = bitesType;
            copy.bitesOffset = bitesOffset;
            copy.effects = new ArrayList<>();
            if (effects != null) effects.forEach(e -> copy.effects.add(e.copy()));
            return copy;
        }

        public Optional<FoodProperties> toFoodProperties(@Nullable FoodProperties fallback) {
            int finalNutrition = nutrition != null ? nutrition : fallback != null ? fallback.nutrition() : 0;
            float finalSaturation = saturation != null ? saturation : fallback != null ? fallback.saturation() : 0F;
            float finalEatSeconds = eatSeconds != null ? eatSeconds : fallback != null ? fallback.eatSeconds() : 1.6F;
            boolean finalCanAlwaysEat = canAlwaysEat != null ? canAlwaysEat : fallback != null && fallback.canAlwaysEat();
            Optional<ItemStack> finalConvert = parseConvert().or(() -> fallback == null ? Optional.empty() : fallback.usingConvertsTo());
            List<FoodProperties.PossibleEffect> finalEffects = buildEffects();
            if (finalEffects.isEmpty() && fallback != null) finalEffects = fallback.effects();
            if (finalNutrition == 0 && finalSaturation == 0F && finalConvert.isEmpty() && finalEffects.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new FoodProperties(finalNutrition, finalSaturation, finalCanAlwaysEat, finalEatSeconds, finalConvert, finalEffects));
        }

        private Optional<ItemStack> parseConvert() {
            if (usingConvertsTo == null || usingConvertsTo.isBlank()) return Optional.empty();
            try {
                ResourceLocation id = ResourceLocation.parse(usingConvertsTo);
                if (!BuiltInRegistries.ITEM.containsKey(id)) return Optional.empty();
                return Optional.of(BuiltInRegistries.ITEM.get(id).getDefaultInstance());
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        private List<FoodProperties.PossibleEffect> buildEffects() {
            if (effects == null || effects.isEmpty()) return List.of();
            List<FoodProperties.PossibleEffect> out = new ArrayList<>();
            for (EffectEntry effect : effects) {
                effect.toPossibleEffect().ifPresent(out::add);
            }
            return out;
        }
    }

    public static final class EffectEntry {
        public String effect = "minecraft:speed";
        public int duration = 200;
        public int amplifier = 0;
        public float probability = 1.0F;
        public boolean ambient = false;
        public boolean visible = true;
        public boolean showIcon = true;

        public EffectEntry sanitized() {
            if (duration < 1) duration = 1;
            if (amplifier < 0) amplifier = 0;
            if (!Float.isFinite(probability)) probability = 1F;
            probability = Math.max(0F, Math.min(1F, probability));
            if (effect == null || effect.isBlank()) effect = "minecraft:speed";
            return this;
        }

        public EffectEntry copy() {
            EffectEntry copy = new EffectEntry();
            copy.effect = effect;
            copy.duration = duration;
            copy.amplifier = amplifier;
            copy.probability = probability;
            copy.ambient = ambient;
            copy.visible = visible;
            copy.showIcon = showIcon;
            return copy;
        }

        public Optional<FoodProperties.PossibleEffect> toPossibleEffect() {
            try {
                ResourceLocation id = ResourceLocation.parse(effect);
                ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, id);
                Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(key);
                return holder.map(mobEffectReference -> new FoodProperties.PossibleEffect(
                        () -> new MobEffectInstance(mobEffectReference, duration, amplifier, ambient, visible, showIcon),
                        probability
                ));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }
}
