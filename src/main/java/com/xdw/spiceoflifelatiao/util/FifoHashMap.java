package com.xdw.spiceoflifelatiao.util;


import java.util.LinkedHashMap;
import java.util.Map;
/// 作为定长缓存区的有序KV队列使用，并附带哈希查找功能
public class FifoHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public FifoHashMap(int maxSize) {
        super(maxSize + 1, 0.75f, false);
        this.maxSize = maxSize;
    }

    @Override
    public synchronized V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized V get(Object key) {
        return super.get(key);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
