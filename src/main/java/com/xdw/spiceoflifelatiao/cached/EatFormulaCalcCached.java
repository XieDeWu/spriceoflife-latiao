package com.xdw.spiceoflifelatiao.cached;

import com.xdw.spiceoflifelatiao.util.EatFormulaContext;
import com.xdw.spiceoflifelatiao.util.FifoHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

public class EatFormulaCalcCached {
    private static final Random random = new Random(0);
    private static long cachedID = random.nextLong();
    // 缓存键改为 Long，避免字符串
    public static final Map<Long,EatFormulaContext> cachedContext = new FifoHashMap<>(64);


    // 纯整数运算，零字符串，零临时对象
    public static long makeKey(@NotNull Player player, @NotNull ItemStack stack, int flag) {
        // 玩家 UUID 的 hashCode 已经够区分，但为了安全可以取 UUID 的 least+most bits
        long playerBits = player.getId();
        // 物品的注册名哈希
        int itemHash = System.identityHashCode(stack.getItem());
        // 游戏时间分片
        long gap = LevelCalcCached.gameTime / 6;
        // 组合：利用各部分的异或和移位，产生 long 键
        // 注意：cachedID 改变会使所有旧缓存自动失效
        return cachedID ^ (playerBits * 31 + itemHash) ^ (gap * 31 + flag);
    }

    public static void refreshCached() {
        cachedID = random.nextLong();
        cachedContext.clear();   // 简单清空，避免旧键残留（因为 cachedID 变了，旧键已经无法匹配）
    }
}
