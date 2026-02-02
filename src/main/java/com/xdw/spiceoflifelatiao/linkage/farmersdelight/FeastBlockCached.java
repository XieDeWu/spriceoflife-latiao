package com.xdw.spiceoflifelatiao.linkage.farmersdelight;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class FeastBlockCached {
    public static boolean flag = false;
    public static Optional<ItemStack> takeServing = Optional.empty();

    public static void start() {
        flag = true;
    }

    public static void end() {
        flag = false;
        takeServing = Optional.empty();
    }
}
