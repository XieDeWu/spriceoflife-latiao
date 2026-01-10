package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PlayerSleepCached {
    private static final FifoHashMap<Integer,Long> timeCached = new FifoHashMap<>(64);
    private static final FifoHashMap<Integer,Float> lossCached = new FifoHashMap<>(64);
    public static void addSampling(Integer id,Long time,Float loss){
        timeCached.put(id,time);
        lossCached.put(id,loss);
    }
    public static @NotNull Optional<Long> getTime(Integer id){
        return Optional.ofNullable(timeCached.get(id));
    }
    public static @NotNull Optional<Float> getLoss(Integer id){
        return Optional.ofNullable(lossCached.get(id));
    }
}
