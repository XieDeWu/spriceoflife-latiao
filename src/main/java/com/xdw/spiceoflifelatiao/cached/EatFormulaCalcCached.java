package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class EatFormulaCalcCached {
    private static final FifoHashMap<String,Optional<EatFormulaContext>> values = new FifoHashMap<>(64);

    public static void addCached(Player player, ItemStack stack, Optional<EatFormulaContext> value,int flag){
        if(player == null || stack == null) return;
        values.put(getID(player,stack,flag),value);
    }
    public static Optional<EatFormulaContext> getCached(Player player, ItemStack stack,int flag){
        if(player == null || stack == null) return Optional.empty();
        return values.getOrDefault(getID(player,stack,flag),Optional.empty());
    }
    private static String getID(Player player,ItemStack stack,int flag){
        var play = player.getStringUUID().hashCode();
        var itemID = stack.getItem().toString().replace(" ", "").hashCode();
        var gap = LevelCalcCached.gameTime /6;
        return play + ":" + itemID + ":" + gap + ":" + flag;
    }

}
