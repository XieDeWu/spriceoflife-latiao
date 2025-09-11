package com.xdw.spiceoflifelatiao.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class LevelCalcCached {
    public static float light = 0f;
    public static void update(Player player){
        Level level = player.level();
        float rainLevel = level.getRainLevel(0);
        float thunderLevel = level.getThunderLevel(0);
        float dayTime = (float) (level.getTimeOfDay(0)+0.25)%1f;
        double day = dayTime * Math.PI * 2;
        double night = day + Math.PI;
        float baseSkyLight = level.getBrightness(LightLayer.SKY, player.blockPosition());
        float blockLight = level.getBrightness(LightLayer.BLOCK, player.blockPosition());

        float skyLight = (float) (baseSkyLight / (1+rainLevel+thunderLevel) * Math.max(Math.sin(day),0.6 * level.getMoonBrightness() *Math.sin(night)));
        light = Math.max(blockLight,skyLight);

    }
}
