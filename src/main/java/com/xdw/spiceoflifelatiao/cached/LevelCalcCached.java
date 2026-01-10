package com.xdw.spiceoflifelatiao.cached;

import net.minecraft.world.level.Level;

public class LevelCalcCached {
    public static long gameTime = 0L;
    public static float rainLevel = 0f;
    public static float thunderLevel = 0f;
    public static float timeOfDay = 0;
    public static float moonBrightness = 0;
    public static void update(Level level){
        rainLevel = level.getRainLevel(0);
        thunderLevel = level.getThunderLevel(0);
        gameTime = level.getGameTime();
        timeOfDay = level.getTimeOfDay(0);
        moonBrightness = level.getMoonBrightness();
    }
}
