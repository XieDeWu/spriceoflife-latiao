package com.xdw.spiceoflifelatiao.linkage;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class FoodItemEvent {
    @SubscribeEvent
    public static void onUsingItem(LivingEntityUseItemEvent.Start event) {
        if(!(event.getItem().getItem() instanceof IFoodItem fi)) return;
        fi.initState();
    }
}
