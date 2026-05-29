package com.yc.z_common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 LinkedHashMap 实现 LRU 缓存
 * LRU：最近最少使用，淘汰最久未访问的数据
 */
public class LRUCacheByLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    // 缓存最大容量
    private final int capacity;

    /**
     * @param capacity 缓存最大容量
     */
    public LRUCacheByLinkedHashMap(int capacity) {
        // 初始容量、负载因子、accessOrder=true(开启访问排序：true=访问顺序，false=插入顺序)
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    /**
     * 重写该方法：判断是否淘汰旧元素
     * 返回 true：当元素数量超过最大容量，自动移除最久未使用节点
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

}