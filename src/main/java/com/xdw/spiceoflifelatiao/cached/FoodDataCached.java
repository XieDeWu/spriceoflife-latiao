package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.network.AddEatHistoryMsg;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FoodDataCached {
    public static boolean flag = false;
    public static boolean flag_common = false;
    public static boolean readFoodInfo = false;
    public static Optional<Player> player = Optional.empty();
    public static Optional<ItemStack> item = Optional.empty();
    public static Optional<BlockState> state = Optional.empty();
    public static Optional<Integer> bites = Optional.empty();
    public static Optional<Integer> bite = Optional.empty();
    public static Optional<Integer> type = Optional.empty();
    public static Optional<List<ItemStack>> usingConvertsTo = Optional.empty();
    public static Optional<FoodProperties> foodProperties = Optional.empty();
    public static Optional<Integer> addHunger = Optional.empty();
    public static Optional<Float> addSaturation = Optional.empty();
    public static Optional<Integer> realHunger = Optional.empty();
    public static Optional<Float> realSaturation = Optional.empty();
    public static Optional<Float> hungerRoundErr = Optional.empty();
    public static Optional<String> blockTagId = Optional.empty();
    public static int accessOrderGetValue = 0;
    public static int accessOrderAdd = 0;
    public static AtomicInteger numSeq = new AtomicInteger(1);
    public static void start(Optional<Player> _player,Optional<ItemStack> _item){
        flag = true;
        player = _player;
        item = _item;
    }
    public static void end(){
//        添加饮食记录 一般饮食行为
        if (player.isPresent()
                && player.get() instanceof ServerPlayer serverPlayer
                && serverPlayer.getFoodData() instanceof IEatHistoryAcessor acc
                && item.isPresent() && realHunger.isPresent()
                && realSaturation.isPresent()
        ) {
            int foodHash = EatHistory.getFoodHash(item.get().getItem());
            PacketDistributor.sendToPlayer(serverPlayer, new AddEatHistoryMsg(foodHash, (float) realHunger.get(), realSaturation.get(), 1.0f / (float) bites.orElse(1), hungerRoundErr.orElse(0F)));
            acc.addEatHistory_Mem(foodHash, (float) realHunger.get(), realSaturation.get(), 1.0f / (float) bites.orElse(1), hungerRoundErr.orElse(0F));
        }
        //            方块食物与分装食物
        if (player.isPresent() && player.get() instanceof ServerPlayer serverPlayer
                && item.isPresent() && bite.isPresent() && bites.isPresent()) {
            var level = serverPlayer.serverLevel();
            var oldData = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);

            // 创建新实例，避免因引用相同导致框架不触发同步
            LevelOrgFoodValue newData = new LevelOrgFoodValue();
            newData.hash.addAll(oldData.hash);
            newData.hunger.putAll(oldData.hunger);
            newData.saturation.putAll(oldData.saturation);
            newData.bites.putAll(oldData.bites);
            newData.bitesOffset.putAll(oldData.bitesOffset);
            newData.bitesType.putAll(oldData.bitesType);
            // 深拷贝内层 Map
            oldData.usingConvertsTo.forEach((k, v) -> newData.usingConvertsTo.put(k, new HashMap<>(v)));

            var defHash = LevelOrgFoodValue.getFoodHash(item.get().getItem(), null,blockTagId.orElse(null));
            var curHash = LevelOrgFoodValue.getFoodHash(item.get().getItem(), bite.get(), blockTagId.orElse(null));

            AtomicBoolean isChanged = new AtomicBoolean(false);

            // === hash ===
            if (!newData.hash.contains(defHash)) {
                newData.hash.add(defHash);
                isChanged.set(true);
            }
            if (!newData.hash.contains(curHash)) {
                newData.hash.add(curHash);
                isChanged.set(true);
            }

            // === hunger ===
            addHunger.ifPresent(newHunger -> {
                Float oldHunger = newData.hunger.get(curHash);
                if (!Objects.equals(oldHunger, newHunger.floatValue())) {
                    newData.hunger.put(curHash, newHunger.floatValue());
                    isChanged.set(true);
                }
            });

            // === saturation ===
            addSaturation.ifPresent(newSaturation -> {
                Float oldSaturation = newData.saturation.get(curHash);
                if (!Objects.equals(oldSaturation, newSaturation)) {
                    newData.saturation.put(curHash, newSaturation);
                    isChanged.set(true);
                }
            });

            // === bites ===
            int newBites = FoodDataCached.bites.get();
            if (!Objects.equals(newData.bites.get(defHash), newBites)) {
                newData.bites.put(defHash, newBites);
                isChanged.set(true);
            }

            // === bitesOffset ===
            int newOffset = FoodDataCached.accessOrderAdd == 1 ? 1 : 0;
            if (!Objects.equals(newData.bitesOffset.get(defHash), newOffset)) {
                newData.bitesOffset.put(defHash, newOffset);
                isChanged.set(true);
            }

            // === bitesType ===
            type.ifPresent(newType -> {
                var oldType = newData.bitesType.get(defHash);
                if (!Objects.equals(oldType, newType)) {
                    newData.bitesType.put(defHash, newType);
                    isChanged.set(true);
                }
            });

            // === usingConvertsTo ===
            FoodDataCached.usingConvertsTo.ifPresent(stacks -> {
                // 将 List<ItemStack> 构建为保序的 TreeMap（按 ResourceLocation 自然序）
                TreeMap<ResourceLocation, Integer> newMap = new TreeMap<>();
                for (ItemStack stack : stacks) {
                    ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    newMap.merge(rl, stack.getCount(), Integer::sum);
                }
                Map<ResourceLocation, Integer> oldMap = newData.usingConvertsTo.get(curHash);
                TreeMap<ResourceLocation, Integer> oldSorted =
                        oldMap != null ? new TreeMap<>(oldMap) : new TreeMap<>();
                if (!Objects.equals(oldSorted, newMap)) {
                    newData.usingConvertsTo.put(curHash, new TreeMap<>(newMap));
                    isChanged.set(true);
                }
            });

            if (isChanged.get()) {
                level.setData(ModAttachments.LEVEL_ORG_FOOD_VALUE, newData);
            }
        }


//        可直接食用的方块食物
        isFlagOk().ifPresent(it->{
//            为方块食物第一口添加洋葱版食物多样性
            if(!FoodDataCached.flag_common && (bite.isEmpty() || bite.get() == 0)){
                var copy = item.get().copy();
                copy.set(DataComponents.FOOD, new FoodProperties(
                        addHunger.orElse(0) * FoodDataCached.bites.orElse(1),
                        addSaturation.orElse(0f) * FoodDataCached.bites.orElse(1),
                        foodProperties.map(FoodProperties::canAlwaysEat).orElse(false),
                        foodProperties.map(FoodProperties::eatSeconds).orElse(1.6f),
                        foodProperties.flatMap(FoodProperties::usingConvertsTo),
                        foodProperties.map(FoodProperties::effects).orElse(List.of())
                ));
                EventHooks.onItemUseFinish(player.get(), copy, 0, ItemStack.EMPTY);
            }
        });
        initFlag();
    }
    public static Optional<IEatHistoryAcessor> isFlagOk(){
        return player.isPresent()
                && item.isPresent()
//                && bites.isPresent()
//                && bite.isPresent()
//                && context.isPresent()
                && realHunger.isPresent()
                && realSaturation.isPresent()
                && hungerRoundErr.isPresent()
                && player.get().getFoodData() instanceof IEatHistoryAcessor ac
                ? Optional.of(ac) : Optional.empty();
    }
    public static void initFlag(){
        flag = false;
        readFoodInfo = false;
        player = Optional.empty();
        item = Optional.empty();
        state = Optional.empty();
        bites = Optional.empty();
        bite = Optional.empty();
        type = Optional.empty();
        usingConvertsTo = Optional.empty();
        foodProperties = Optional.empty();
        addHunger = Optional.empty();
        addSaturation = Optional.empty();
        realHunger = Optional.empty();
        realSaturation = Optional.empty();
        hungerRoundErr = Optional.empty();
        blockTagId = Optional.empty();
        accessOrderGetValue = 0;
        accessOrderAdd = 0;
        numSeq = new AtomicInteger(1);
        EatFormulaCalcCached.refreshCached();
    }

}
