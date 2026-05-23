package com.xdw.spiceoflifelatiao.event;

import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.cached.ConfigCached;
import com.xdw.spiceoflifelatiao.config.ManualFoodConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Applies config/spiceoflifelatiao/edible_items.json to the real default item components.
 *
 * The older logic in LevelOrgFoodValue/IItemExtensionMixin can still calculate dynamic/world-specific
 * values, but tools such as Jade, AppleSkin and vanilla interaction code commonly check the FOOD data
 * component directly.  Setting DataComponents.FOOD here makes manually configured items standard food
 * after a restart, which is deterministic and does not require a runtime sync packet.
 */
public final class ManualFoodComponentEvent {
    private ManualFoodComponentEvent() {}

    public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        if (!ConfigCached.ENABLE_MANUAL_FOOD_FILE) return;

        // Ensure the file exists and is parsed before default components are frozen.
        ManualFoodConfig.load();

        int patched = 0;
        for (Map.Entry<String, ManualFoodConfig.Entry> raw : ManualFoodConfig.startupComponentEntries().entrySet()) {
            ResourceLocation itemId = tryParse(raw.getKey()).orElse(null);
            if (itemId == null) {
                SpiceOfLifeLatiao.LOGGER.warn("Skipping invalid manual food item id in {}: {}", ManualFoodConfig.path(), raw.getKey());
                continue;
            }
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                SpiceOfLifeLatiao.LOGGER.warn("Skipping unknown manual food item in {}: {}", ManualFoodConfig.path(), itemId);
                continue;
            }

            Item item = BuiltInRegistries.ITEM.get(itemId);
            FoodProperties fallback = item.getDefaultInstance().get(DataComponents.FOOD);
            Optional<FoodProperties> food = raw.getValue().toFoodProperties(fallback);
            if (food.isEmpty()) {
                SpiceOfLifeLatiao.LOGGER.warn("Skipping manual food item with empty effective food values in {}: {}", ManualFoodConfig.path(), itemId);
                continue;
            }

            event.modify(item, builder -> builder.set(DataComponents.FOOD, food.get()));
            patched++;
        }

        if (patched > 0) {
            SpiceOfLifeLatiao.LOGGER.info("Applied {} manual edible item(s) to default FOOD components from {}", patched, ManualFoodConfig.path());
        }
    }

    private static Optional<ResourceLocation> tryParse(String id) {
        try {
            return Optional.of(ResourceLocation.parse(id));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
