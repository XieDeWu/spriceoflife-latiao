package com.xdw.spiceoflifelatiao.event;

import com.xdw.spiceoflifelatiao.attachments.ModAttachments;
import com.xdw.spiceoflifelatiao.attachments.PlayerUnSleepTimeRecord;
import com.xdw.spiceoflifelatiao.cached.*;
import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

public class PlayerEventHandle {
    // 玩家动作消耗
    public static final HashMap<UUID, HashMap<String,Map.Entry<Float,Integer>>> playerActionsLoss = new HashMap<>();
    public static final HashMap<UUID, Vec3> playerPosCached = new HashMap<>();

    @SubscribeEvent
    public static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        var id = player.getStringUUID().hashCode();
        PlayerAfkCached.addSampling(id,player.getLookAngle(), LevelCalcCached.gameTime);
        if(PlayerAfkCached.isAfk(id, LevelCalcCached.gameTime,PlayerCalcCached.hunger)) return;
        LevelCalcCached.update(player.level());
        PlayerCalcCached.update(player);
        if(ConfigCached.ENABLE_CHANGE && ConfigCached.ENABLE_LOSS){
            EatFormulaContext.from(player, ItemStack.EMPTY,null,0).ifPresent(x->player.causeFoodExhaustion(x.loss()));
        }
        if(LevelCalcCached.gameTime % 20 == 0){
            long oldTime = player.level().isClientSide
                    ? player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get()).player_un_sleeptime()
                    : Optional.ofNullable(ModAttachments.server_cached_player_un_sleeptime.get(id))
                    .orElseGet(()->player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get()).player_un_sleeptime());
            var newTime = oldTime + 1;
            newTime = player.isSleeping() ? Math.round(Math.max(0,newTime*0.98-160)) : newTime;
            if(player.level().isClientSide){
                player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(), new PlayerUnSleepTimeRecord(newTime));
            }else{
                ModAttachments.server_cached_player_un_sleeptime.put(id,newTime);
                if(LevelCalcCached.gameTime % 200 == 0) {
                    player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(), new PlayerUnSleepTimeRecord(newTime));
                }
            }
        }

        /// 动作消耗 移动 跳跃 游泳 攀爬 点击 使用 飞行
        boolean posChanged = Optional.ofNullable(playerPosCached.get(player.getUUID()))
                .map(it -> it.distanceTo(player.position()) > 0.01D)
                .orElse(true);
        playerPosCached.put(player.getUUID(), player.position());
        BiConsumer<String,Float> regAction = (name,loss)-> playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(name, Map.entry(loss, 20));
        // 移动
        if (posChanged
                && !player.isCrouching()
                && !player.isSprinting()
                && !player.isSwimming()
                && !player.isFallFlying()
                && !player.isPassenger()
                && player.onGround()
        ) regAction.accept("move", (float) ConfigCached.ACTION_MOVE);
        // 跳跃 事件外部注入
        // 游泳
        if (posChanged && player.isSwimming()) regAction.accept("swim", (float) ConfigCached.ACTION_SWIM);
        // 攀爬
        if (posChanged && player.onClimbable()) regAction.accept("climb", (float) ConfigCached.ACTION_CLIMB);
        // 点击 由外部事件注入
        // 使用 由外部事件注入 此处补充持续使用
        if(player.isUsingItem()) regAction.accept("use", (float) ConfigCached.ACTION_USE);
        // 飞行
        if(posChanged && player.isFallFlying() && !player.onGround()) regAction.accept("flying", (float) ConfigCached.ACTION_FLYING);

        // 结算疲劳 数据来源取决于对playerActionsLoss的填充方
        var actions = playerActionsLoss.computeIfAbsent(player.getUUID(),it->new HashMap<>());
        var actionsLoss = actions.values().stream()
                .filter(it->it.getValue() > 0)
                .map(Map.Entry::getKey)
                .reduce(0F, Float::sum);
        actions.entrySet().removeIf(entry -> entry.getValue().getValue() <= 0);
        actions.replaceAll((action, entry) -> Map.entry(entry.getKey(), entry.getValue() - 1));
        if(ConfigCached.ENABLE_ACTIONS_LOSS) player.causeFoodExhaustion(actionsLoss);
    }


    @SubscribeEvent
    public static void canPlayerSleepEvent(CanPlayerSleepEvent event){
        var player = event.getEntity();
        var id = player.getStringUUID().hashCode();
        var time = player.level().getDayTime();
        var loss = EatFormulaContext.from(player,ItemStack.EMPTY,null,(int)LevelCalcCached.gameTime).map(EatFormulaContext::loss).orElse(0f);
        PlayerSleepCached.addSampling(id,time,loss);
    }
    @SubscribeEvent
    public static void onPlayerWalkUp(PlayerWakeUpEvent event){
        var player = event.getEntity();
        var id = player.getStringUUID().hashCode();
        PlayerSleepCached
                .getTime(id).flatMap(time -> PlayerSleepCached.getLoss(id).map(loss -> Map.entry(time, loss)))
                .ifPresent(entry -> {
                    long dayTime = player.level().getDayTime();
                    long gap = Math.clamp(dayTime - entry.getKey(), 0, 72000);
                    float fatigue = gap * entry.getValue();
                    player.causeFoodExhaustion(fatigue);

                    var time = player.getData(ModAttachments.PLAYER_UN_SLEEPTIME.get());
                    var updTime = new PlayerUnSleepTimeRecord(Math.round(Math.max(0,time.player_un_sleeptime() * 0.7 - gap * 6)));
                    ModAttachments.server_cached_player_un_sleeptime.put(id,updTime.player_un_sleeptime());
                    player.setData(ModAttachments.PLAYER_UN_SLEEPTIME.get(),updTime);
                });
    }
    // 点击（左键攻击瞬间）
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put("click", Map.entry((float) ConfigCached.ACTION_CLICK, 20));
    }

    // 点击(方块)
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put("click", Map.entry((float) ConfigCached.ACTION_CLICK, 20));
    }

    // 点击(空挥)
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put("click", Map.entry((float) ConfigCached.ACTION_CLICK, 20));
    }


    // 使用(方块)
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put("use", Map.entry((float) ConfigCached.ACTION_USE, 20));
    }

    // 使用(物品)
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        playerActionsLoss.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put("use", Map.entry((float) ConfigCached.ACTION_USE, 20));
    }

}
