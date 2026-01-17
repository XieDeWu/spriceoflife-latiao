package com.xdw.spiceoflifelatiao.linkage.solcarrot;

import java.util.Optional;

public class FoodTrackerCached {
    public static boolean flag = false;
    public static Optional<Boolean> hasTriedNewFood = Optional.empty();
    public static void start(){
        flag = true;
    }
    public static void end(){
        flag = false;
        hasTriedNewFood = Optional.empty();
    }
}
