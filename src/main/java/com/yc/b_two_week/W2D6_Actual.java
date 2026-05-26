package com.yc.b_two_week;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 对比 Hashtable 与 ConcurrentHashMap 多线程并发写入性能
 */
public class W2D6_Actual {
    // 每个线程写入 1 万条数据
    private static final int COUNT_PER_THREAD = 10000;
    // 开启 10 个线程并发写
    private static final int THREAD_COUNT = 10;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== 多线程并发 put 性能对比 =====");
        System.out.println("线程数：" + THREAD_COUNT + "，每线程写入：" + COUNT_PER_THREAD);
        System.out.println();

        // 1. 测试 Hashtable
        Map<String, Integer> hashtable = new Hashtable<>();
        long hashtableTime = testMap(hashtable, "Hashtable");

        // 2. 测试 ConcurrentHashMap
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        long concurrentTime = testMap(concurrentMap, "ConcurrentHashMap");

        System.out.println("\n===== 最终结论 =====");
        System.out.println("Hashtable 耗时：" + hashtableTime + " ms");
        System.out.println("ConcurrentHashMap 耗时：" + concurrentTime + " ms");
        System.out.println("ConcurrentHashMap 比 Hashtable 快：" + (hashtableTime - concurrentTime) + " ms");




    // 验证 ConcurrentHashMap 迭代器的弱一致性：遍历过程中插入新元素
        // 1. 初始化 ConcurrentHashMap
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");

        System.out.println("===== 开始遍历 ConcurrentHashMap =====");

        // 2. 获取迭代器（此时会生成一个快照/视图）
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();

        // 3. 启动一个新线程：在遍历过程中插入新数据
        new Thread(() -> {
            try {
                Thread.sleep(100); // 确保先开始遍历
                System.out.println("\n【线程2】正在插入新数据：key4, value4");
                map.put("key4", "value4"); // 遍历中插入
                System.out.println("【线程2】插入完成！");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // 4. 主线程遍历（慢一点，让插入发生在遍历中）
        while (iterator.hasNext()) {
            Thread.sleep(200);
            Map.Entry<String, String> entry = iterator.next();
            System.out.println("遍历：" + entry.getKey() + " = " + entry.getValue());
        }

        System.out.println("\n===== 遍历结束 =====");
        System.out.println("最终 map 内容：" + map);
        System.out.println("迭代器是否看到新数据？：NO（弱一致性）");
        System.out.println("是否抛并发修改异常？：NO（弱一致性保障）");


    }

    /**
     * 测试并发写入，返回总耗时（毫秒）
     */
    private static long testMap(Map<String, Integer> map, String name) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        long start = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < COUNT_PER_THREAD; j++) {
                        String key = threadId + "-" + j;
                        map.put(key, j);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有线程执行完成
        latch.await();
        long end = System.currentTimeMillis();
        long time = end - start;

        System.out.println(name + " 执行完成，耗时：" + time + " ms");
        return time;
    }
}