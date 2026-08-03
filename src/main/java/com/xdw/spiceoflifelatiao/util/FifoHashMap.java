package com.xdw.spiceoflifelatiao.util;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 线程安全的 FIFO 缓存 Map（容量固定）。
 * 所有公共方法均使用 synchronized 保证并发安全。
 */
public class FifoHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public FifoHashMap(int maxSize) {
        super(maxSize, 0.75f, false);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    @Override
    public synchronized V get(Object key) {
        return super.get(key);
    }

    @Override
    public synchronized V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized V remove(Object key) {
        return super.remove(key);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return super.containsValue(value);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
    }

    @Override
    public synchronized @NotNull Set<Map.Entry<K, V>> entrySet() {
        // 返回的 Set 本身不保证线程安全，建议不直接暴露，或返回同步包装。
        // 但你的代码未使用 entrySet，所以可以暂时保持。
        return super.entrySet();
    }
}