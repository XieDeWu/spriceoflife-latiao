package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.minecraft.world.phys.Vec3;

public class PlayerAfkCached {
    private static final FifoHashMap<Integer,Vec3> playerAfkAngle = new FifoHashMap<>(64);
    private static final FifoHashMap<Integer,Long> playerAfkTime = new FifoHashMap<>(64);
    public static void addSampling(Integer id,Vec3 angle,Long curTime){
        if(angle.distanceToSqr(playerAfkAngle.getOrDefault(id,Vec3.ZERO)) > 1.0E-4){
            playerAfkAngle.put(id,angle);
            playerAfkTime.put(id,curTime);
        }
    }
    public static boolean isAfk(Integer id, Long curTime, int hunger){
//        20gt 60s 4min~?
        var afkTime  = playerAfkTime.getOrDefault(id,Long.MAX_VALUE);
        return (curTime - afkTime) > 20L * 60L * (3 + hunger);
    }
}
