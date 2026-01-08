package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class FoodPropertiesCached {
    private static final FifoHashMap<String, Optional<FoodProperties>> values = new FifoHashMap<>(64);

    public static Optional<FoodProperties> getCached(LivingEntity _player, ItemStack stack){
        if(!(_player instanceof Player player) || stack == null) return Optional.empty();
        var id = getID(player,stack);
        return values.getOrDefault(id,Optional.empty());
    }

    public static void addCached(Player player,ItemStack stack, FoodProperties value){
        values.put(getID(player,stack),Optional.ofNullable(value));
    }

    private static String getID(Player player,ItemStack stack){
        var play = player.getStringUUID().hashCode();
        var itemID = stack.getItem().toString().replace(" ", "").hashCode();
        var gap = player.level().getGameTime()/6;
        return play + ":" + itemID + ":" + gap;
    }
}
