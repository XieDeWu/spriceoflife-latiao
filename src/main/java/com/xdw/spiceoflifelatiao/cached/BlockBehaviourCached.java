package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.EatHistory;
import com.xdw.spiceoflifelatiao.util.IEatHistoryAcessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class BlockBehaviourCached {
    public static boolean flag = false;
    public static Optional<Player> player = Optional.empty();
    public static Optional<ItemStack> item = Optional.empty();
    public static Optional<Integer> bites = Optional.empty();
    private static Optional<EatFormulaContext> context = Optional.empty();
    public static Optional<Integer> realHunger = Optional.empty();
    public static Optional<Float> realSaturation = Optional.empty();
    public static Optional<Float> hungerRoundErr = Optional.empty();
    public static void start(Optional<Player> _player,Optional<ItemStack> _item){
        flag = true;
        player = _player;
        item = _item;
    }
    public static void end(){
        if (player.isPresent()
                && item.isPresent()
                && bites.isPresent()
                && context.isPresent()
                && realHunger.isPresent()
                && realSaturation.isPresent()
                && hungerRoundErr.isPresent()
                && player.get().getFoodData() instanceof IEatHistoryAcessor ac
        ){
            int hash = EatHistory.getFoodHash(item.get().getItem());
            ac.addEatHistory_Mem(hash, (float)realHunger.get(), realSaturation.get(), 1.0f/(float)bites.get(), hungerRoundErr.get());
        }
        flag = false;
        player = Optional.empty();
        item = Optional.empty();
        bites = Optional.empty();
        context = Optional.empty();
        realHunger = Optional.empty();
        realSaturation = Optional.empty();
        hungerRoundErr = Optional.empty();
    }
    public static Optional<EatFormulaContext> getContext(){
        if (context.isPresent()) return context;
        if (player.isPresent() && item.isPresent()){
            context = EatFormulaContext.from(player.get(),item.get(),item.get().getFoodProperties(player.get()));
            return context;
        }
        return Optional.empty();
    }
    public static void foodUpd(int hunger, float saturation) {
        item.ifPresent(x->{
            Optional<FoodProperties> old = Optional.ofNullable(x.get(DataComponents.FOOD));
            x.set(DataComponents.FOOD,new FoodProperties(
                    hunger,
                    saturation,
                    old.map(FoodProperties::canAlwaysEat).orElse(false),
                    old.map(FoodProperties::eatSeconds).orElse(1.6f),
                    old.flatMap(FoodProperties::usingConvertsTo),
                    old.map(FoodProperties::effects).orElse(List.of())
            ));
        });
    }
}
