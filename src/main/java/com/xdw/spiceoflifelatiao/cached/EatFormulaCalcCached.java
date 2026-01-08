package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
        var itemID = stack.getItem().toString().replace(" ", "").hashCode();
        var gap = player.level().getGameTime()/6;
        return play + ":" + itemID + ":" + gap;
    }

}
