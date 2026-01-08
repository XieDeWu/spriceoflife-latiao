package com.xdw.spiceoflifelatiao.util;


import java.util.LinkedHashMap;
import java.util.Map;
/// 作为定长缓存区的有序KV队列使用，并附带哈希查找功能
public class FifoHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public FifoHashMap(int maxSize) {
        // accessOrder=false 保持插入顺序
        super(maxSize + 1, 0.75f, false);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // 超过容量时，自动删除最老元素
        return size() > maxSize;
    }
}