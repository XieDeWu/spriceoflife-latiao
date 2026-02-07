package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.network.AddEatHistoryMsg;
import com.xdw.spiceoflifelatiao.network.EatHistoryMsg;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockBehaviourCached {
    public static boolean flag = false;
    public static Optional<Player> player = Optional.empty();
    public static Optional<ItemStack> item = Optional.empty();
    public static Optional<Integer> bites = Optional.empty();
    public static Optional<Integer> bite = Optional.empty();
    public static Optional<Integer> type = Optional.empty();
    public static Optional<ItemStack> usingConvertsTo = Optional.empty();
    public static Optional<FoodProperties> foodProperties = Optional.empty();
    public static Optional<Integer> addHunger = Optional.empty();
    public static Optional<Float> addSaturation = Optional.empty();
    public static Optional<Integer> realHunger = Optional.empty();
    public static Optional<Float> realSaturation = Optional.empty();
    public static Optional<Float> hungerRoundErr = Optional.empty();
    private static Optional<Vec3> context = Optional.empty();
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
        if (player.isPresent() && player.get() instanceof ServerPlayer serverPlayer && serverPlayer.getFoodData() instanceof IEatHistoryAcessor acc && item.isPresent() && realHunger.isPresent() && realSaturation.isPresent()) {
            int foodHash = EatHistory.getFoodHash(item.get().getItem());
            PacketDistributor.sendToPlayer(serverPlayer, new AddEatHistoryMsg(foodHash, (float) realHunger.get(), realSaturation.get(), 1.0f / (float) bites.orElse(1), hungerRoundErr.orElse(0F)));
            acc.addEatHistory_Mem(foodHash, (float) realHunger.get(), realSaturation.get(), 1.0f / (float) bites.orElse(1), hungerRoundErr.orElse(0F));
        }
//            方块食物与分装食物
        if (player.isPresent() && item.isPresent() && bite.isPresent() && bites.isPresent()) {
            var defHash = LevelOrgFoodValue.getFoodHash(item.get().getItem(), null);
            var curHash = LevelOrgFoodValue.getFoodHash(item.get().getItem(), bite.get());
            var level = player.get().level();
            var data = level.getData(ModAttachments.LEVEL_ORG_FOOD_VALUE);

            AtomicBoolean isChanged = new AtomicBoolean(false);

            // === hash ===
            if (!data.hash.contains(defHash)) {
                data.hash.add(defHash);
                isChanged.set(true);
            }
            if (!data.hash.contains(curHash)) {
                data.hash.add(curHash);
                isChanged.set(true);
            }

            // === hunger ===
            addHunger.ifPresent(newHunger -> {
                Float oldHunger = data.hunger.get(curHash);
                if (!Objects.equals(oldHunger, newHunger.floatValue())) {
                    data.hunger.put(curHash, newHunger.floatValue());
                    isChanged.set(true);
                }
            });

            // === saturation ===
            addSaturation.ifPresent(newSaturation -> {
                Float oldSaturation = data.saturation.get(curHash);
                if (!Objects.equals(oldSaturation, newSaturation)) {
                    data.saturation.put(curHash, newSaturation);
                    isChanged.set(true);
                }
            });

            // === bites ===
            int newBites = BlockBehaviourCached.bites.get();
            if (!Objects.equals(data.bites.get(defHash), newBites)) {
                data.bites.put(defHash, newBites);
                isChanged.set(true);
            }

            // === bitesOffset ===
            int newOffset = BlockBehaviourCached.accessOrderAdd == 1 ? 1 : 0;
            if (!Objects.equals(data.bitesOffset.get(defHash), newOffset)) {
                data.bitesOffset.put(defHash, newOffset);
                isChanged.set(true);
            }

            // === bitesType ===
            type.ifPresent(newType -> {
                var oldType = data.bitesType.get(defHash);
                if (!Objects.equals(oldType, newType)) {
                    data.bitesType.put(defHash, newType);
                    isChanged.set(true);
                }
            });

            // === usingConvertsTo ===
            BlockBehaviourCached.usingConvertsTo.ifPresent(stack -> {
                ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
                ResourceLocation old = data.usingConvertsTo.get(curHash);
                if (!Objects.equals(old, rl)) {
                    data.usingConvertsTo.put(curHash, rl);
                    isChanged.set(true);
                }
            });

            if (isChanged.get()) {
                level.setData(ModAttachments.LEVEL_ORG_FOOD_VALUE, data);
            }
        }


//        可直接食用的方块食物
        isFlagOk().ifPresent(it->{
//            为方块食物第一口添加洋葱版食物多样性
            if(bite.isEmpty() || bite.get() == 0){
                item.get().set(DataComponents.FOOD,new FoodProperties(
                        addHunger.orElse(0)* BlockBehaviourCached.bites.orElse(1),
                        addSaturation.orElse(0f)* BlockBehaviourCached.bites.orElse(1),
                        foodProperties.map(FoodProperties::canAlwaysEat).orElse(false),
                        foodProperties.map(FoodProperties::eatSeconds).orElse(1.6f),
                        foodProperties.flatMap(FoodProperties::usingConvertsTo),
                        foodProperties.map(FoodProperties::effects).orElse(List.of())
                ));
                EventHooks.onItemUseFinish(player.get(), item.get(), 0, ItemStack.EMPTY);
            }
        });
        initFlag();
    }
    public static Optional<IEatHistoryAcessor> isFlagOk(){
        return player.isPresent()
                && item.isPresent()
//                && bites.isPresent()
//                && bite.isPresent()
                && context.isPresent()
                && realHunger.isPresent()
                && realSaturation.isPresent()
                && hungerRoundErr.isPresent()
                && player.get().getFoodData() instanceof IEatHistoryAcessor ac
                ? Optional.of(ac) : Optional.empty();
    }
    public static void initFlag(){
        flag = false;
        player = Optional.empty();
        item = Optional.empty();
        bites = Optional.empty();
        bite = Optional.empty();
        type = Optional.empty();
        usingConvertsTo = Optional.empty();
        foodProperties = Optional.empty();
        context = Optional.empty();
        addHunger = Optional.empty();
        addSaturation = Optional.empty();
        realHunger = Optional.empty();
        realSaturation = Optional.empty();
        hungerRoundErr = Optional.empty();
        accessOrderGetValue = 0;
        accessOrderAdd = 0;
        numSeq = new AtomicInteger(1);
    }

    public static Optional<Vec3> getContext(FoodProperties defaultFoodInfo,int flag){
        if (context.isPresent()) return context;
        if (player.isPresent() && item.isPresent()){
            context = Optional.of(LevelOrgFoodValue.getBlockFoodInfo(BlockBehaviourCached.player.get(),BlockBehaviourCached.item.get(),0,defaultFoodInfo,false,flag));
            return context;
        }

        return Optional.empty();
    }
}
