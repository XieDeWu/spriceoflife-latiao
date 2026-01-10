package com.xdw.spiceoflifelatiao.event;

import net.neoforged.bus.api.SubscribeEvent;

public class LevelEventHandle {
    @SubscribeEvent
    public static void tickLevel(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
//        LevelCalcCached.update(event.getLevel());
    }
}
