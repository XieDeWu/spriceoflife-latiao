package com.xdw.spiceoflifelatiao.linkage.jade;

import com.xdw.spiceoflifelatiao.SpiceOfLifeLatiao;
import com.xdw.spiceoflifelatiao.attachments.LevelOrgFoodValue;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.impl.ui.ElementHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class FoodInfoPlugin implements IWailaPlugin, IBlockComponentProvider {
    public static final ResourceLocation FOOD_INFO = ResourceLocation.fromNamespaceAndPath(SpiceOfLifeLatiao.MODID, "food_info");
    public static final ResourceLocation RES_HUNGER = ResourceLocation.withDefaultNamespace("hud/food_full");
    public static final ResourceLocation RES_HUNGER_HALF = ResourceLocation.withDefaultNamespace("hud/food_half");
    public static final ResourceLocation RES_APPLESKIN = ResourceLocation.fromNamespaceAndPath("appleskin", "textures/icons.png");
    public static final IElement ICON_HUNGER = ElementHelper.INSTANCE.sprite(RES_HUNGER, 9, 9);
    public static final IElement ICON_HUNGER_HALF = ElementHelper.INSTANCE.sprite(RES_HUNGER_HALF, 9, 9);
    public static final IElement ICON_SATURATION = new CustomSpriteElement(RES_APPLESKIN, 21, 27, 7, 7, 256, 256);
    public static final IElement ICON_SATURATION_HALF_MORE = new CustomSpriteElement(RES_APPLESKIN, 14, 27, 7, 7, 256, 256);
    public static final IElement ICON_SATURATION_HALF = new CustomSpriteElement(RES_APPLESKIN, 7, 27, 7, 7, 256, 256);
    public static final IElement ICON_SATURATION_QRT = new CustomSpriteElement(RES_APPLESKIN, 0, 27, 7, 7, 256, 256);
    public static Optional<Player> player = Optional.empty();
    public static Optional<ItemStack> stack = Optional.empty();
    public static Optional<Integer> bite = Optional.empty();

    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, Block.class);
        registration.addRayTraceCallback(10000, (HitResult hitResult, @Nullable Accessor<?> accessor, @Nullable Accessor<?> originalAccessor) -> {
            if (accessor == null) return null;
            if (!(accessor instanceof BlockAccessor acc)) return null;
            init();
            player = Optional.ofNullable(accessor.getPlayer());
            if (player.isEmpty()) return null;
            stack = Optional.of(accessor.getPickedResult());
            bite = acc.getBlockState()
                    .getProperties()
                    .stream()
                    .filter(p -> p instanceof IntegerProperty ip && "bites".equals(ip.getName()))
                    .map(p -> acc.getBlockState().getValue((IntegerProperty) p))
                    .findFirst();
            return accessor;
        });
    }

    public void init() {
        player = Optional.empty();
        stack = Optional.empty();
        bite = Optional.empty();
    }

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (player.isEmpty() || stack.isEmpty() || bite.isEmpty()) return;
        var checked = LevelOrgFoodValue.checkBlockFoodInfo(player.get(), stack.get());
        var itemFoodInfo = !checked ? stack.get().get(DataComponents.FOOD) : null;
        Vec3 blockFoodInfo = LevelOrgFoodValue.getBlockFoodInfo(player.get(), stack.get(), itemFoodInfo, bite.get());
        List<@NotNull IElement> hud_hunger = new ArrayList<>();
        List<@NotNull IElement> hud_saturation = new ArrayList<>();
        List<@NotNull IElement> hud_warn = new ArrayList<>();
        if (blockFoodInfo.x > 0 || blockFoodInfo.y > 0) {
            long ignored1 = Stream.iterate(Math.round(blockFoodInfo.x), s -> s > 0F, s -> {
                if (s > 20) {
                    hud_hunger.add(ICON_HUNGER);
                    hud_hunger.add(ElementHelper.INSTANCE.spacer(2,9));
                    hud_hunger.add(ElementHelper.INSTANCE.text(Component.literal("x" + s/2).withStyle(ChatFormatting.GOLD)));
                    return 0L;
                } else if (s >= 2) {
                    hud_hunger.add(ICON_HUNGER);
                    return s - 2;
                }
                hud_hunger.add(ICON_HUNGER_HALF);
                return s - 2;
            }).count();
            var hud2 = Stream.iterate(blockFoodInfo.y, s -> s > 0F, s -> {
                if (s > 20) {
                    hud_saturation.add(ICON_SATURATION);
                    hud_saturation.add(ElementHelper.INSTANCE.spacer(2,7));
                    hud_saturation.add(ElementHelper.INSTANCE.text(Component.literal("x" + Math.round(s/2)).withStyle(ChatFormatting.GOLD)));
                    return 0D;
                } else if (s >= 2) {
                    hud_saturation.add(ICON_SATURATION);
                    return s - 2;
                } else if (s >= 1.5) {
                    hud_saturation.add(ICON_SATURATION_HALF_MORE);
                    return s - 2;
                } else if (s >= 1) {
                    hud_saturation.add(ICON_SATURATION_HALF);
                    return s - 2;
                }
                hud_saturation.add(ICON_SATURATION_QRT);
                return s - 2;
            });
            if(Minecraft.getInstance().getResourceManager().getResource(RES_APPLESKIN).isPresent()) hud2.count();
        }
        var ex = itemFoodInfo != null ? "此方块饮食数据未采集"+",默认显示物品数据" : "";
        if(!checked) {
            hud_warn.add(ElementHelper.INSTANCE.text(Component.translatable(itemFoodInfo == null
                    ? "spiceoflifelatiao.tooltip.block_food.uncollected"
                    : "spiceoflifelatiao.tooltip.block_food.uncollected.food_item"
            ).withStyle(ChatFormatting.DARK_RED)));
        }
        iTooltip.add(hud_hunger);
        iTooltip.add(hud_saturation);
        iTooltip.add(hud_warn);
    }

    @Override
    public ResourceLocation getUid() {
        return FOOD_INFO;
    }
}
