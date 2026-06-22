package com.xdw.spiceoflifelatiao.cached;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FoodPropertiesCached {

    private static final Map<Integer, FoodProperties> values = new HashMap<>(128);
    private static final int MAX_SIZE = 96;

    public static Optional<FoodProperties> getCached(LivingEntity _player, ItemStack stack) {
        if (!(_player instanceof Player player) || stack == null) return Optional.empty();
        if (values.size() > MAX_SIZE) values.clear();
        int key = createKey(player, stack);
        // 单次 get，null 即为未命中
        return Optional.ofNullable(values.get(key));
    }

    public static void addCached(Player player, ItemStack stack, FoodProperties value) {
        int key = createKey(player, stack);
        if (value != null) {
            values.put(key, value);
        }
        // value 为 null 时不写入，避免后续 get 误判为命中
    }

    private static int createKey(Player player, ItemStack stack) {
        int h = player.getId();
        h = 31 * h + System.identityHashCode(stack.getItem());
        h = 31 * h + (int) LevelCalcCached.gameTime / 6;
        return h;
    }
}