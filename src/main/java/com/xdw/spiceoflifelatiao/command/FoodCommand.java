package com.xdw.spiceoflifelatiao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.config.ManualFoodConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FoodCommand {
    private static final int PAGE_SIZE = 8;

    private FoodCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        registerRoot(event.getDispatcher(), "sol_latiao");
        registerRoot(event.getDispatcher(), "spiceoflifelatiao");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("food")
                        .then(Commands.literal("path").executes(ctx -> path(ctx.getSource())))
                        .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                        .then(Commands.literal("save").executes(ctx -> save(ctx.getSource())))
                        .then(Commands.literal("list")
                                .then(Commands.literal("current")
                                        .executes(ctx -> listCurrent(ctx.getSource(), 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listCurrent(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))))
                                .then(Commands.literal("global")
                                        .executes(ctx -> listGlobal(ctx.getSource(), 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listGlobal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))))
                                .then(Commands.literal("all")
                                        .executes(ctx -> listAll(ctx.getSource(), 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> listAll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"))))))
                        .then(Commands.literal("show")
                                .then(Commands.literal("current").then(itemArg("item").executes(ctx -> show(ctx.getSource(), ctx.getSource().getLevel(), itemId(ctx, "item")))))
                                .then(Commands.literal("global").then(itemArg("item").executes(ctx -> showGlobal(ctx.getSource(), itemId(ctx, "item")))))
                                .then(Commands.literal("all").then(itemArg("item").executes(ctx -> showAll(ctx.getSource(), itemId(ctx, "item"))))))
                        .then(Commands.literal("add")
                                .then(Commands.literal("current").then(addArgs(false)))
                                .then(Commands.literal("global").then(addArgs(true))))
                        .then(Commands.literal("remove")
                                .then(Commands.literal("current").then(itemArg("item").executes(ctx -> remove(ctx.getSource(), ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item")))))
                                .then(Commands.literal("global").then(itemArg("item").executes(ctx -> remove(ctx.getSource(), null, itemId(ctx, "item"))))))
                        .then(Commands.literal("set")
                                .then(Commands.literal("current").then(setArgs(false)))
                                .then(Commands.literal("global").then(setArgs(true))))
                        .then(Commands.literal("effect")
                                .then(Commands.literal("add")
                                        .then(Commands.literal("current").then(effectAddArgs(false)))
                                        .then(Commands.literal("global").then(effectAddArgs(true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.literal("current").then(effectRemoveArgs(false)))
                                        .then(Commands.literal("global").then(effectRemoveArgs(true))))
                                .then(Commands.literal("clear")
                                        .then(Commands.literal("current").then(itemArg("item").executes(ctx -> clearEffects(ctx.getSource(), ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item")))))
                                        .then(Commands.literal("global").then(itemArg("item").executes(ctx -> clearEffects(ctx.getSource(), null, itemId(ctx, "item")))))))
                        .then(Commands.literal("importLegacy")
                                .then(Commands.literal("current").executes(ctx -> importLegacyCurrent(ctx.getSource())))
                                .then(Commands.literal("all").executes(ctx -> importLegacyAll(ctx.getSource()))))
                ));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> itemArg(String name) {
        return Commands.argument(name, ResourceLocationArgument.id());
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> addArgs(boolean global) {
        return itemArg("item")
                .then(Commands.argument("nutrition", IntegerArgumentType.integer(0))
                        .then(Commands.argument("saturation", FloatArgumentType.floatArg(0F))
                                .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), 1.6F, null, null, null, null, null))
                                .then(Commands.argument("eatSeconds", FloatArgumentType.floatArg(0.05F))
                                        .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), FloatArgumentType.getFloat(ctx, "eatSeconds"), null, null, null, null, null))
                                        .then(Commands.argument("canAlwaysEat", BoolArgumentType.bool())
                                                .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), FloatArgumentType.getFloat(ctx, "eatSeconds"), BoolArgumentType.getBool(ctx, "canAlwaysEat"), null, null, null, null))
                                                .then(itemArg("usingConvertsTo")
                                                        .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), FloatArgumentType.getFloat(ctx, "eatSeconds"), BoolArgumentType.getBool(ctx, "canAlwaysEat"), rawId(ctx, "usingConvertsTo"), null, null, null))
                                                        .then(Commands.argument("bites", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), FloatArgumentType.getFloat(ctx, "eatSeconds"), BoolArgumentType.getBool(ctx, "canAlwaysEat"), rawId(ctx, "usingConvertsTo"), IntegerArgumentType.getInteger(ctx, "bites"), null, null))
                                                                .then(Commands.argument("bitesType", StringArgumentType.word())
                                                                        .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), FloatArgumentType.getFloat(ctx, "eatSeconds"), BoolArgumentType.getBool(ctx, "canAlwaysEat"), rawId(ctx, "usingConvertsTo"), IntegerArgumentType.getInteger(ctx, "bites"), StringArgumentType.getString(ctx, "bitesType"), null))
                                                                        .then(Commands.argument("bitesOffset", IntegerArgumentType.integer(0))
                                                                                .executes(ctx -> add(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), IntegerArgumentType.getInteger(ctx, "nutrition"), FloatArgumentType.getFloat(ctx, "saturation"), FloatArgumentType.getFloat(ctx, "eatSeconds"), BoolArgumentType.getBool(ctx, "canAlwaysEat"), rawId(ctx, "usingConvertsTo"), IntegerArgumentType.getInteger(ctx, "bites"), StringArgumentType.getString(ctx, "bitesType"), IntegerArgumentType.getInteger(ctx, "bitesOffset")))))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> setArgs(boolean global) {
        return itemArg("item")
                .then(setNutritionLiteral(global, "nutrition"))
                .then(setNutritionLiteral(global, "hunger"))
                .then(setNutritionLiteral(global, "hungry"))
                .then(Commands.literal("saturation").then(Commands.argument("value", FloatArgumentType.floatArg(0F)).executes(ctx -> setEntry(ctx.getSource(), global, itemId(ctx, "item"), e -> e.saturation = FloatArgumentType.getFloat(ctx, "value"), "saturation"))))
                .then(Commands.literal("eatSeconds").then(Commands.argument("value", FloatArgumentType.floatArg(0.05F)).executes(ctx -> setEntry(ctx.getSource(), global, itemId(ctx, "item"), e -> e.eatSeconds = FloatArgumentType.getFloat(ctx, "value"), "eatSeconds"))))
                .then(Commands.literal("canAlwaysEat").then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> setEntry(ctx.getSource(), global, itemId(ctx, "item"), e -> e.canAlwaysEat = BoolArgumentType.getBool(ctx, "value"), "canAlwaysEat"))))
                .then(Commands.literal("usingConvertsTo").then(itemArg("value").executes(ctx -> setConvert(ctx.getSource(), global, itemId(ctx, "item"), rawId(ctx, "value")))))
                .then(Commands.literal("bites").then(Commands.argument("value", IntegerArgumentType.integer(1)).executes(ctx -> setEntry(ctx.getSource(), global, itemId(ctx, "item"), e -> e.bites = IntegerArgumentType.getInteger(ctx, "value"), "bites"))))
                .then(Commands.literal("bitesOffset").then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(ctx -> setEntry(ctx.getSource(), global, itemId(ctx, "item"), e -> e.bitesOffset = IntegerArgumentType.getInteger(ctx, "value"), "bitesOffset"))))
                .then(Commands.literal("bitesType").then(Commands.argument("value", StringArgumentType.word()).executes(ctx -> setBitesType(ctx.getSource(), global, itemId(ctx, "item"), StringArgumentType.getString(ctx, "value")))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> setNutritionLiteral(boolean global, String literal) {
        return Commands.literal(literal)
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(ctx -> setEntry(ctx.getSource(), global, itemId(ctx, "item"), e -> e.nutrition = IntegerArgumentType.getInteger(ctx, "value"), literal)));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> effectAddArgs(boolean global) {
        return itemArg("item")
                .then(itemArg("effect")
                        .then(Commands.argument("durationTicks", IntegerArgumentType.integer(1))
                                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0))
                                        .executes(ctx -> addEffect(ctx.getSource(), global, itemId(ctx, "item"), effectId(ctx, "effect"), IntegerArgumentType.getInteger(ctx, "durationTicks"), IntegerArgumentType.getInteger(ctx, "amplifier"), 1F))
                                        .then(Commands.argument("probability", FloatArgumentType.floatArg(0F, 1F))
                                                .executes(ctx -> addEffect(ctx.getSource(), global, itemId(ctx, "item"), effectId(ctx, "effect"), IntegerArgumentType.getInteger(ctx, "durationTicks"), IntegerArgumentType.getInteger(ctx, "amplifier"), FloatArgumentType.getFloat(ctx, "probability")))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> effectRemoveArgs(boolean global) {
        return itemArg("item")
                .then(itemArg("effect").executes(ctx -> removeEffect(ctx.getSource(), global ? null : ctx.getSource().getLevel().dimension().location(), itemId(ctx, "item"), effectId(ctx, "effect"))));
    }

    private static String rawId(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name) {
        return ResourceLocationArgument.getId(ctx, name).toString();
    }

    private static ResourceLocation itemId(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name) {
        return resolveParsedId(ResourceLocationArgument.getId(ctx, name), ManualFoodConfig::isValidItem);
    }

    private static ResourceLocation effectId(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name) {
        return resolveParsedId(ResourceLocationArgument.getId(ctx, name), ManualFoodConfig::isValidEffect);
    }

    private static ResourceLocation resolveId(String raw, Predicate<ResourceLocation> exists) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        ResourceLocation direct = tryParseId(value);
        if (direct == null) return ResourceLocation.parse(value);
        return resolveParsedId(direct, exists);
    }

    private static ResourceLocation resolveParsedId(ResourceLocation parsed, Predicate<ResourceLocation> exists) {
        if (parsed == null) return ResourceLocation.parse("");
        if (exists.test(parsed)) return parsed;

        // Vanilla's ResourceLocationArgument turns a bare word like "mekanism_canteen" into
        // "minecraft:mekanism_canteen". Treat that minecraft path as an alias candidate.
        if ("minecraft".equals(parsed.getNamespace())) {
            ResourceLocation alias = resolveAlias(parsed.getPath(), exists);
            if (alias != null) return alias;
        }

        // Also allow manually supplied raw strings to fall back through this method.
        ResourceLocation alias = resolveAlias(parsed.toString(), exists);
        if (alias != null) return alias;
        return parsed;
    }

    private static ResourceLocation resolveAlias(String value, Predicate<ResourceLocation> exists) {
        if (value == null || value.indexOf('_') < 0) return null;
        List<ResourceLocation> candidates = new ArrayList<>();
        for (int i = value.indexOf('_'); i >= 0; i = value.indexOf('_', i + 1)) {
            if (i <= 0 || i >= value.length() - 1) continue;
            ResourceLocation candidate = tryParseId(value.substring(0, i) + ":" + value.substring(i + 1));
            if (candidate != null && exists.test(candidate)) candidates.add(candidate);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingInt(id -> -id.getNamespace().length()));
        return candidates.get(0);
    }

    private static ResourceLocation tryParseId(String value) {
        try {
            return ResourceLocation.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int path(CommandSourceStack source) {
        success(source, "手动可吃配置文件：" + ManualFoodConfig.path().toAbsolutePath());
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        ManualFoodConfig.load();
        int synced = syncAllManualEntries(source);
        success(source, "已重新读取手动可吃配置：" + ManualFoodConfig.path().toAbsolutePath() + "，已同步 " + synced + " 个已加载世界条目到原世界附件");
        return 1;
    }

    private static int save(CommandSourceStack source) {
        ManualFoodConfig.save();
        success(source, "已保存手动可吃配置：" + ManualFoodConfig.path().toAbsolutePath());
        return 1;
    }

    private static int add(CommandSourceStack source, ResourceLocation dimension, ResourceLocation itemId, int nutrition, float saturation, float eatSeconds, Boolean canAlwaysEat, String convertRaw, Integer bites, String bitesTypeRaw, Integer bitesOffset) {
        if (!ManualFoodConfig.isValidItem(itemId)) return fail(source, "未知物品：" + itemId);
        ManualFoodConfig.Entry entry = new ManualFoodConfig.Entry();
        entry.enabled = true;
        entry.nutrition = nutrition;
        entry.saturation = saturation;
        entry.eatSeconds = eatSeconds;
        entry.canAlwaysEat = canAlwaysEat;
        if (convertRaw != null && !convertRaw.isBlank() && !isClearToken(convertRaw)) {
            ResourceLocation convert = resolveId(convertRaw, ManualFoodConfig::isValidItem);
            if (!ManualFoodConfig.isValidItem(convert)) return fail(source, "未知转换物品：" + convertRaw + "（解析为 " + convert + "）");
            entry.usingConvertsTo = convert.toString();
        }
        entry.bites = bites;
        entry.bitesOffset = bitesOffset;
        if (bitesTypeRaw != null) {
            Integer type = parseBitesType(bitesTypeRaw);
            if (type == null) return fail(source, "bitesType 只能是 bites/0 或 servings/1");
            entry.bitesType = type;
        }
        put(dimension, itemId, entry);
        resyncManualItem(source, dimension, itemId);
        success(source, "已添加手动可吃物品：" + scopeName(dimension) + " " + ManualFoodConfig.describeEntry(itemId, entry));
        return 1;
    }

    private static void put(ResourceLocation dimension, ResourceLocation itemId, ManualFoodConfig.Entry entry) {
        if (dimension == null) ManualFoodConfig.putGlobal(itemId, entry);
        else ManualFoodConfig.putWorld(dimension, itemId, entry);
    }

    private static int remove(CommandSourceStack source, ResourceLocation dimension, ResourceLocation itemId) {
        boolean removed = dimension == null ? ManualFoodConfig.removeGlobal(itemId) : ManualFoodConfig.removeWorld(dimension, itemId);
        if (!removed) return fail(source, "没有找到手动条目：" + scopeName(dimension) + " " + itemId);
        resyncManualItem(source, dimension, itemId);
        success(source, "已删除手动条目：" + scopeName(dimension) + " " + itemId);
        return 1;
    }

    private static int setEntry(CommandSourceStack source, boolean global, ResourceLocation itemId, Consumer<ManualFoodConfig.Entry> updater, String field) {
        if (!ManualFoodConfig.isValidItem(itemId)) return fail(source, "未知物品：" + itemId);
        ResourceLocation dimension = global ? null : source.getLevel().dimension().location();
        ManualFoodConfig.Entry entry = ManualFoodConfig.getOrCreate(dimension, itemId);
        updater.accept(entry);
        entry.sanitized();
        ManualFoodConfig.save();
        resyncManualItem(source, dimension, itemId);
        success(source, "已修改 " + scopeName(dimension) + " " + itemId + " 的 " + field);
        return 1;
    }

    private static int setConvert(CommandSourceStack source, boolean global, ResourceLocation itemId, String raw) {
        if (!ManualFoodConfig.isValidItem(itemId)) return fail(source, "未知物品：" + itemId);
        ResourceLocation dimension = global ? null : source.getLevel().dimension().location();
        ManualFoodConfig.Entry entry = ManualFoodConfig.getOrCreate(dimension, itemId);
        if (isClearToken(raw)) {
            entry.usingConvertsTo = null;
        } else {
            ResourceLocation convert = resolveId(raw, ManualFoodConfig::isValidItem);
            if (!ManualFoodConfig.isValidItem(convert)) return fail(source, "未知转换物品：" + raw + "（解析为 " + convert + "）");
            entry.usingConvertsTo = convert.toString();
        }
        ManualFoodConfig.save();
        resyncManualItem(source, dimension, itemId);
        success(source, "已修改 " + scopeName(dimension) + " " + itemId + " 的 usingConvertsTo");
        return 1;
    }

    private static int setBitesType(CommandSourceStack source, boolean global, ResourceLocation itemId, String raw) {
        Integer type = parseBitesType(raw);
        if (type == null) return fail(source, "bitesType 只能是 bites/0 或 servings/1");
        return setEntry(source, global, itemId, e -> e.bitesType = type, "bitesType");
    }


    private static boolean isClearToken(String raw) {
        if (raw == null) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.equals("clear") || value.equals("none") || value.equals("-")
                || value.equals("minecraft:clear") || value.equals("minecraft:none") || value.equals("minecraft:-");
    }

    private static Integer parseBitesType(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.equals("bites") || lower.equals("bite") || lower.equals("0")) return 0;
        if (lower.equals("servings") || lower.equals("serving") || lower.equals("1")) return 1;
        return null;
    }

    private static int addEffect(CommandSourceStack source, boolean global, ResourceLocation itemId, ResourceLocation effectId, int duration, int amplifier, float probability) {
        if (!ManualFoodConfig.isValidItem(itemId)) return fail(source, "未知物品：" + itemId);
        if (!ManualFoodConfig.isValidEffect(effectId)) return fail(source, "未知效果：" + effectId);
        ResourceLocation dimension = global ? null : source.getLevel().dimension().location();
        ManualFoodConfig.Entry entry = ManualFoodConfig.getOrCreate(dimension, itemId);
        ManualFoodConfig.EffectEntry effect = new ManualFoodConfig.EffectEntry();
        effect.effect = effectId.toString();
        effect.duration = duration;
        effect.amplifier = amplifier;
        effect.probability = probability;
        entry.effects.add(effect.sanitized());
        ManualFoodConfig.save();
        resyncManualItem(source, dimension, itemId);
        success(source, "已添加效果：" + scopeName(dimension) + " " + itemId + " -> " + effectId + " " + duration + "t amp=" + amplifier + " p=" + probability);
        return 1;
    }

    private static int removeEffect(CommandSourceStack source, ResourceLocation dimension, ResourceLocation itemId, ResourceLocation effectId) {
        Optional<ManualFoodConfig.Entry> opt = dimension == null
                ? Optional.ofNullable(ManualFoodConfig.globalItems().get(itemId.toString()))
                : Optional.ofNullable(ManualFoodConfig.worldItems(dimension).get(itemId.toString()));
        if (opt.isEmpty()) return fail(source, "没有找到手动条目：" + scopeName(dimension) + " " + itemId);
        ManualFoodConfig.Entry entry = ManualFoodConfig.getOrCreate(dimension, itemId);
        int before = entry.effects.size();
        entry.effects.removeIf(e -> effectId.toString().equals(e.effect));
        ManualFoodConfig.save();
        resyncManualItem(source, dimension, itemId);
        success(source, "已删除 " + (before - entry.effects.size()) + " 个效果：" + scopeName(dimension) + " " + itemId + " -> " + effectId);
        return 1;
    }

    private static int clearEffects(CommandSourceStack source, ResourceLocation dimension, ResourceLocation itemId) {
        ManualFoodConfig.Entry entry = ManualFoodConfig.getOrCreate(dimension, itemId);
        int before = entry.effects.size();
        entry.effects.clear();
        ManualFoodConfig.save();
        resyncManualItem(source, dimension, itemId);
        success(source, "已清空 " + scopeName(dimension) + " " + itemId + " 的效果，共 " + before + " 个");
        return 1;
    }

    private static int listCurrent(CommandSourceStack source, int page) {
        ServerLevel level = source.getLevel();
        List<String> lines = new ArrayList<>(collectLevel(level, true).values());
        return sendPage(source, "当前世界 " + level.dimension().location() + " 可吃名单", lines, page);
    }

    private static int listGlobal(CommandSourceStack source, int page) {
        List<String> lines = ManualFoodConfig.globalItems().entrySet().stream()
                .map(e -> "[manual/global] " + ManualFoodConfig.describeEntry(ResourceLocation.parse(e.getKey()), e.getValue()))
                .toList();
        return sendPage(source, "全局手动可吃名单", lines, page);
    }

    private static int listAll(CommandSourceStack source, int page) {
        List<String> lines = new ArrayList<>();
        ManualFoodConfig.globalItems().forEach((id, entry) -> lines.add("[manual/global] " + ManualFoodConfig.describeEntry(ResourceLocation.parse(id), entry)));
        for (ServerLevel level : source.getServer().getAllLevels()) {
            collectLevel(level, false).values().forEach(line -> lines.add("[" + level.dimension().location() + "] " + line));
        }
        return sendPage(source, "所有已加载世界可吃名单（手动/自动，省略重复的原版食物）", lines, page);
    }

    private static SortedMap<String, String> collectLevel(ServerLevel level, boolean includeVanilla) {
        SortedMap<String, String> lines = new TreeMap<>();
        ResourceLocation dimension = level.dimension().location();
        ManualFoodConfig.globalItems().forEach((id, entry) -> lines.put(id + "#global", "[manual/global] " + ManualFoodConfig.describeEntry(ResourceLocation.parse(id), entry)));
        ManualFoodConfig.worldItems(dimension).forEach((id, entry) -> lines.put(id + "#world", "[manual/world] " + ManualFoodConfig.describeEntry(ResourceLocation.parse(id), entry)));

        LevelOrgFoodValue data = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(id);
            FoodProperties defaultFood = item.getDefaultInstance().get(DataComponents.FOOD);
            if (LevelOrgFoodValue.isTrusted(data, item, null)) {
                lines.putIfAbsent(id + "#auto", "[auto/world] " + describeAuto(id, item, data));
            } else if (includeVanilla && defaultFood != null) {
                lines.putIfAbsent(id + "#vanilla", "[vanilla] " + describeFood(id, defaultFood));
            }
        }
        return lines;
    }

    private static String describeAuto(ResourceLocation id, Item item, LevelOrgFoodValue data) {
        int defHash = LevelOrgFoodValue.getFoodHash(item, null);
        StringBuilder sb = new StringBuilder(id.toString());
        if (data.bites.containsKey(defHash)) sb.append(" bites=").append(data.bites.get(defHash));
        if (data.bitesType.containsKey(defHash)) sb.append(" type=").append(data.bitesType.get(defHash) == 1 ? "servings" : "bites");
        if (data.bitesOffset.containsKey(defHash)) sb.append(" offset=").append(data.bitesOffset.get(defHash));
        int hash0 = LevelOrgFoodValue.getFoodHash(item, data.bitesType.getOrDefault(defHash, 0) == 1 ? data.bites.getOrDefault(defHash, 0) : 0);
        if (data.hunger.containsKey(hash0)) sb.append(" hunger=").append(data.hunger.get(hash0));
        if (data.saturation.containsKey(hash0)) sb.append(" saturation=").append(data.saturation.get(hash0));
        if (data.usingConvertsTo.containsKey(hash0)) sb.append(" convert=").append(data.usingConvertsTo.get(hash0));
        return sb.toString();
    }

    private static String describeFood(ResourceLocation id, FoodProperties food) {
        StringBuilder sb = new StringBuilder(id.toString());
        sb.append(" hunger=").append(food.nutrition());
        sb.append(" saturation=").append(food.saturation());
        sb.append(" eatSeconds=").append(food.eatSeconds());
        if (food.canAlwaysEat()) sb.append(" alwaysEat=true");
        food.usingConvertsTo().ifPresent(stack -> sb.append(" convert=").append(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        if (!food.effects().isEmpty()) sb.append(" effects=").append(food.effects().size());
        return sb.toString();
    }

    private static int show(CommandSourceStack source, ServerLevel level, ResourceLocation itemId) {
        Optional<Item> item = ManualFoodConfig.item(itemId);
        if (item.isEmpty()) return fail(source, "未知物品：" + itemId);
        List<String> lines = new ArrayList<>();
        FoodProperties fallback = item.get().getDefaultInstance().get(DataComponents.FOOD);
        lines.add("物品：" + itemId + " / 世界：" + level.dimension().location());
        lines.add("原版FOOD：" + (fallback == null ? "无" : describeFood(itemId, fallback)));
        ManualFoodConfig.getEntry(level, itemId).ifPresentOrElse(
                entry -> lines.add("手动配置：" + describeManualEffective(itemId, item.get(), entry) + describeEffects(entry)),
                () -> lines.add("手动配置：无")
        );
        LevelOrgFoodValue data = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
        lines.add("自动学习：" + (LevelOrgFoodValue.isTrusted(data, item.get(), null) ? describeAuto(itemId, item.get(), data) : "无/未信任"));
        lines.forEach(line -> success(source, line));
        return 1;
    }

    private static int showGlobal(CommandSourceStack source, ResourceLocation itemId) {
        Optional<Item> item = ManualFoodConfig.item(itemId);
        if (item.isEmpty()) return fail(source, "未知物品：" + itemId);
        Optional<ManualFoodConfig.Entry> entry = Optional.ofNullable(ManualFoodConfig.globalItems().get(itemId.toString()));
        entry.ifPresentOrElse(
                value -> success(source, "全局手动配置：" + describeManualEffective(itemId, item.get(), value) + describeEffects(value)),
                () -> fail(source, "没有全局手动配置：" + itemId)
        );
        return entry.isPresent() ? 1 : 0;
    }

    private static int showAll(CommandSourceStack source, ResourceLocation itemId) {
        int shown = showGlobal(source, itemId);
        for (ServerLevel level : source.getServer().getAllLevels()) shown += show(source, level, itemId);
        return shown;
    }

    private static String describeManualEffective(ResourceLocation itemId, Item item, ManualFoodConfig.Entry entry) {
        FoodProperties fallback = item.getDefaultInstance().get(DataComponents.FOOD);
        Optional<FoodProperties> food = entry.toFoodProperties(fallback);
        StringBuilder sb = new StringBuilder();
        food.ifPresentOrElse(
                value -> sb.append(describeFood(itemId, value)),
                () -> sb.append(itemId).append(" effective=none")
        );
        if (entry.bites != null) sb.append(" bites=").append(entry.bites);
        if (entry.bitesType != null) sb.append(" type=").append(entry.bitesType == 1 ? "servings" : "bites");
        if (entry.bitesOffset != null) sb.append(" offset=").append(entry.bitesOffset);
        return sb.toString();
    }

    private static String describeEffects(ManualFoodConfig.Entry entry) {
        if (entry.effects == null || entry.effects.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" effects=[");
        for (int i = 0; i < entry.effects.size(); i++) {
            ManualFoodConfig.EffectEntry e = entry.effects.get(i);
            if (i > 0) sb.append(", ");
            sb.append(e.effect).append(" ").append(e.duration).append("t amp=").append(e.amplifier).append(" p=").append(e.probability);
        }
        sb.append("]");
        return sb.toString();
    }

    private static int syncAllManualEntries(CommandSourceStack source) {
        int synced = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            Set<ResourceLocation> ids = new TreeSet<>();
            ManualFoodConfig.globalItems().keySet().forEach(id -> ids.add(ResourceLocation.parse(id)));
            ManualFoodConfig.worldItems(level.dimension().location()).keySet().forEach(id -> ids.add(ResourceLocation.parse(id)));
            for (ResourceLocation id : ids) {
                resyncManualItemToLevel(level, id);
                synced++;
            }
        }
        return synced;
    }

    private static void resyncManualItem(CommandSourceStack source, ResourceLocation dimension, ResourceLocation itemId) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (dimension != null && !level.dimension().location().equals(dimension)) continue;
            resyncManualItemToLevel(level, itemId);
        }
    }

    private static void resyncManualItemToLevel(ServerLevel level, ResourceLocation itemId) {
        Optional<Item> item = ManualFoodConfig.item(itemId);
        if (item.isEmpty()) return;
        Optional<ManualFoodConfig.Entry> entry = ManualFoodConfig.getEntry(level, itemId);
        if (entry.isPresent()) syncManualEntryToAttachment(level, itemId, item.get(), entry.get());
        else clearItemFromAttachment(level, item.get());
    }

    private static void syncManualEntryToAttachment(ServerLevel level, ResourceLocation itemId, Item item, ManualFoodConfig.Entry entry) {
        LevelOrgFoodValue data = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
        FoodProperties fallback = item.getDefaultInstance().get(DataComponents.FOOD);
        int nutrition = entry.nutrition != null ? entry.nutrition : fallback != null ? fallback.nutrition() : 0;
        float saturation = entry.saturation != null ? entry.saturation : fallback != null ? fallback.saturation() : 0F;
        int finalBites = Math.max(1, entry.bites == null ? 1 : entry.bites);
        int type = entry.bitesType == null ? 0 : entry.bitesType;
        int offset = Math.max(0, entry.bitesOffset == null ? 0 : entry.bitesOffset);
        float perBiteHunger = nutrition / (float) finalBites;
        float perBiteSaturation = saturation / (float) finalBites;

        int defHash = LevelOrgFoodValue.getFoodHash(item, null);
        data.hash.add(defHash);
        LevelOrgFoodValue.markTrusted(data, item, null);
        if (entry.bites != null) data.bites.put(defHash, finalBites);
        else data.bites.remove(defHash);
        if (entry.bitesType != null) data.bitesType.put(defHash, type);
        else data.bitesType.remove(defHash);
        if (entry.bitesOffset != null) data.bitesOffset.put(defHash, offset);
        else data.bitesOffset.remove(defHash);

        int first = type == 1 ? 1 : 0;
        int endExclusive = type == 1 ? finalBites + 1 : Math.max(1, finalBites + offset);
        for (int bite = first; bite < endExclusive; bite++) {
            int hash = LevelOrgFoodValue.getFoodHash(item, bite);
            data.hash.add(hash);
            LevelOrgFoodValue.markTrusted(data, item, bite);
            data.hunger.put(hash, perBiteHunger);
            data.saturation.put(hash, perBiteSaturation);
            ResourceLocation convert = tryParseId(entry.usingConvertsTo == null ? "" : entry.usingConvertsTo);
            if (convert != null && ManualFoodConfig.isValidItem(convert)) {
                data.usingConvertsTo.put(hash, convert);
            } else {
                data.usingConvertsTo.remove(hash);
            }
        }

        // Also keep the legacy display hash populated for non-sliced/manual foods.
        int displayHash = LevelOrgFoodValue.getFoodHash(item, type == 1 ? finalBites : 0);
        data.hash.add(displayHash);
        LevelOrgFoodValue.markTrusted(data, item, type == 1 ? finalBites : 0);
        data.hunger.put(displayHash, perBiteHunger);
        data.saturation.put(displayHash, perBiteSaturation);
        ResourceLocation displayConvert = tryParseId(entry.usingConvertsTo == null ? "" : entry.usingConvertsTo);
        if (displayConvert != null && ManualFoodConfig.isValidItem(displayConvert)) {
            data.usingConvertsTo.put(displayHash, displayConvert);
        } else {
            data.usingConvertsTo.remove(displayHash);
        }

        level.setData(ModAttachments.LEVEL_ORG_FOOD_VALUE, data);
    }

    private static void clearItemFromAttachment(ServerLevel level, Item item) {
        LevelOrgFoodValue data = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
        int defHash = LevelOrgFoodValue.getFoodHash(item, null);
        Integer bites = data.bites.get(defHash);
        Integer type = data.bitesType.get(defHash);
        Integer offset = data.bitesOffset.get(defHash);
        Set<Integer> hashes = new HashSet<>();
        hashes.add(defHash);
        hashes.add(LevelOrgFoodValue.getFoodHash(item, 0));
        if (bites != null) {
            int first = Objects.equals(type, 1) ? 1 : 0;
            int endExclusive = Objects.equals(type, 1) ? bites + 1 : Math.max(1, bites + Math.max(0, offset == null ? 0 : offset));
            for (int bite = first; bite < endExclusive; bite++) hashes.add(LevelOrgFoodValue.getFoodHash(item, bite));
        }
        for (Integer hash : hashes) removeHash(data, hash);
        level.setData(ModAttachments.LEVEL_ORG_FOOD_VALUE, data);
    }

    private static void removeHash(LevelOrgFoodValue data, int hash) {
        data.hash.remove(hash);
        data.hunger.remove(hash);
        data.saturation.remove(hash);
        data.bites.remove(hash);
        data.bitesOffset.remove(hash);
        data.bitesType.remove(hash);
        data.usingConvertsTo.remove(hash);
        data.itemIds.remove(hash);
    }

    private static int importLegacyCurrent(CommandSourceStack source) {
        return importLegacy(source, List.of(source.getLevel()));
    }

    private static int importLegacyAll(CommandSourceStack source) {
        List<ServerLevel> levels = new ArrayList<>();
        source.getServer().getAllLevels().forEach(levels::add);
        return importLegacy(source, levels);
    }

    private static int importLegacy(CommandSourceStack source, Collection<ServerLevel> levels) {
        int imported = 0;
        for (ServerLevel level : levels) {
            LevelOrgFoodValue data = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                Item item = BuiltInRegistries.ITEM.get(id);
                int legacyDef = LevelOrgFoodValue.getLegacyFoodHash(item, null);
                int currentDef = LevelOrgFoodValue.getFoodHash(item, null);
                if (!data.hash.contains(legacyDef) && !data.hash.contains(currentDef)) continue;
                ManualFoodConfig.Entry entry = legacyEntryFrom(data, item, legacyDef, currentDef);
                ManualFoodConfig.putWorld(level.dimension().location(), id, entry);
                imported++;
            }
        }
        ManualFoodConfig.save();
        success(source, "已从旧隐藏世界附件导入 " + imported + " 个条目到 " + ManualFoodConfig.path().toAbsolutePath());
        return imported;
    }

    private static ManualFoodConfig.Entry legacyEntryFrom(LevelOrgFoodValue data, Item item, int legacyDef, int currentDef) {
        ManualFoodConfig.Entry entry = new ManualFoodConfig.Entry();
        int defHash = data.hash.contains(legacyDef) ? legacyDef : currentDef;
        entry.bites = data.bites.get(defHash);
        entry.bitesOffset = data.bitesOffset.get(defHash);
        entry.bitesType = data.bitesType.get(defHash);
        int finalBites = entry.bites == null ? 1 : entry.bites;
        int valueBite = entry.bitesType != null && entry.bitesType == 1 ? finalBites : 0;
        int oldValueHash = LevelOrgFoodValue.getLegacyFoodHash(item, valueBite);
        int newValueHash = LevelOrgFoodValue.getFoodHash(item, valueBite);
        Float hunger = data.hunger.getOrDefault(oldValueHash, data.hunger.get(newValueHash));
        Float saturation = data.saturation.getOrDefault(oldValueHash, data.saturation.get(newValueHash));
        if (hunger != null) entry.nutrition = Math.round(hunger * Math.max(1, finalBites));
        if (saturation != null) entry.saturation = saturation * Math.max(1, finalBites);
        ResourceLocation convert = data.usingConvertsTo.getOrDefault(oldValueHash, data.usingConvertsTo.get(newValueHash));
        if (convert != null) entry.usingConvertsTo = convert.toString();
        FoodProperties fallback = item.getDefaultInstance().get(DataComponents.FOOD);
        if (entry.nutrition == null && fallback != null) entry.nutrition = fallback.nutrition();
        if (entry.saturation == null && fallback != null) entry.saturation = fallback.saturation();
        return entry.sanitized();
    }

    private static int sendPage(CommandSourceStack source, String title, List<String> lines, int page) {
        if (lines.isEmpty()) {
            success(source, title + "：空");
            return 0;
        }
        int maxPage = Math.max(1, (int) Math.ceil(lines.size() / (double) PAGE_SIZE));
        int safePage = Math.max(1, Math.min(page, maxPage));
        success(source, title + " 第 " + safePage + "/" + maxPage + " 页，共 " + lines.size() + " 条");
        int from = (safePage - 1) * PAGE_SIZE;
        int to = Math.min(lines.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) success(source, (i + 1) + ". " + lines.get(i));
        return lines.size();
    }

    private static String scopeName(ResourceLocation dimension) {
        return dimension == null ? "global" : dimension.toString();
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
