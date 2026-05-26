package com.yc.Xcommon;

import java.util.List;

public class PECSTest {

    // --------------- PECS 生产者：读取数据 → ? extends T ----------------
    public static void printList(List<? extends Number> list) {
        // 只能读，不能添加（除了 null）
        for (Number num : list) {
            System.out.println(num);
        }
    }

    // --------------- PECS 消费者：写入数据 → ? super T ----------------
    public static void addNumbers(List<? super Integer> list) {
        // 可以添加 Integer 及其子类
        list.add(10);
        list.add(20);
    }
}