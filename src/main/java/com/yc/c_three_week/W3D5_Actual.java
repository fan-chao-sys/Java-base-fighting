package com.yc.c_three_week;

import com.yc.Xcommon.Counter;
import com.yc.Xcommon.ProducerConsumerDemo;

public class W3D5_Actual {

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        // 开启10个线程，每个线程自增1000次
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            }).start();
        }

        Thread.sleep(2000);
        System.out.println("最终计数：" + counter.getCount()); // 结果固定 10000


        // 手写一个生产者消费者模型（用 Lock + Condition 实现）
        ProducerConsumerDemo demo = new ProducerConsumerDemo();
        // 生产者线程
        new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    demo.produce(i);
                    Thread.sleep(200); // 生产慢一点
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "【生产者】").start();

        // 消费者线程
        new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    demo.consume();
                    Thread.sleep(500); // 消费慢一点
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "    【消费者】").start();
    }
}