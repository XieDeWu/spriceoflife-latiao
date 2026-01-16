package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;
import java.util.Optional;

public class BlockBehaviourCached {
    public static boolean flag = false;
    public static Optional<Player> player = Optional.empty();
    public static Optional<ItemStack> item = Optional.empty();
    public static Optional<Integer> bites = Optional.empty();
    public static Optional<Integer> bite = Optional.empty();
    public static Optional<FoodProperties> foodProperties = Optional.empty();
    public static Optional<Integer> addHunger = Optional.empty();
    public static Optional<Float> addSaturation = Optional.empty();
    public static Optional<Integer> realHunger = Optional.empty();
    public static Optional<Float> realSaturation = Optional.empty();
    public static Optional<Float> hungerRoundErr = Optional.empty();
    private static Optional<EatFormulaContext> context = Optional.empty();
    public static void start(Optional<Player> _player,Optional<ItemStack> _item){
        flag = true;
        player = _player;
        item = _item;
    }
    public static void end(){
        isFlagOk().ifPresent(it->{
            int hash = EatHistory.getFoodHash(item.get().getItem());
            it.addEatHistory_Mem(hash, (float)realHunger.get(), realSaturation.get(), 1.0f/(float)bites.get(), hungerRoundErr.get());
//            为方块食物第一口添加洋葱版食物多样性
            if(bite.get() == 0){
                item.get().set(DataComponents.FOOD,new FoodProperties(
                        addHunger.orElse(0)*bites.get(),
                        addSaturation.orElse(0f)*bites.get(),
                        foodProperties.map(FoodProperties::canAlwaysEat).orElse(false),
                        foodProperties.map(FoodProperties::eatSeconds).orElse(1.6f),
                        foodProperties.map(FoodProperties::usingConvertsTo).orElse(Optional.empty()),
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
                && bites.isPresent()
                && bite.isPresent()
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
        foodProperties = Optional.empty();
        context = Optional.empty();
        addHunger = Optional.empty();
        addSaturation = Optional.empty();
        realHunger = Optional.empty();
        realSaturation = Optional.empty();
        hungerRoundErr = Optional.empty();
    }

    public static Optional<EatFormulaContext> getContext(FoodProperties defaultFoodInfo){
        if (context.isPresent()) return context;
        if (player.isPresent() && item.isPresent()){
            context = EatFormulaContext.from(player.get(),item.get(),defaultFoodInfo);
            return context;
        }
        return Optional.empty();
    }
}
