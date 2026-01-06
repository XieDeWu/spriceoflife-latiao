package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class EatFormulaCalcCached {
    private static final FifoHashMap<String,Optional<EatFormulaContext>> values = new FifoHashMap<>(64);

    public static void addCached(Player player, ItemStack stack, Optional<EatFormulaContext> value){
        if(player == null || stack == null) return;
        values.put(getID(player,stack),value);
    }
    public static Optional<EatFormulaContext> getCached(Player player, ItemStack stack){
        if(player == null || stack == null) return Optional.empty();
        return values.getOrDefault(getID(player,stack),Optional.empty());
    }
    private static String getID(Player player,ItemStack stack){
        var play = player.getStringUUID().hashCode();
//        var hunger = player.getFoodData().getFoodLevel();
//        var saturation = player.getFoodData().getSaturationLevel();
        var item = stack.getItem().toString().replace(" ", "").hashCode();
        var gap = player.level().getGameTime()/20;
        return play + ":" + item + ":" + gap;
    }

    static class FifoHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public FifoHashMap(int maxSize) {
            // accessOrder=false 保持插入顺序
            super(maxSize + 1, 0.75f, false);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            // 超过容量时，自动删除最老元素
            return size() > maxSize;
        }
    }
}
