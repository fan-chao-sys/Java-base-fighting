package com.yc.c_three_week;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

public class W3D3_Actual {

    // 锁对象
    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== JVM 信息 =====");
        System.out.println(VM.current().details());
        System.out.println("\n===== 开始验证 synchronized 锁升级 =====");

        // 1. 【无锁状态】：刚创建对象，未加锁
        System.out.println("\n---------- 1. 无锁状态（未加锁） ----------");
        System.out.println(ClassLayout.parseInstance(lock).toPrintable());

        // 2. 【偏向锁状态】：同一个线程第一次加锁
        System.out.println("\n---------- 2. 偏向锁状态（同一线程首次加锁） ----------");
        synchronized (lock) {
            System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }

        // 3. 【轻量级锁状态】：另一个线程竞争锁（偏向锁撤销 → 轻量级锁）
        System.out.println("\n---------- 3. 轻量级锁状态（线程竞争） ----------");
        new Thread(() -> {
            synchronized (lock) {
                // 自旋/轻量级锁
            }
        }).start();
        Thread.sleep(100); // 等待线程执行
        synchronized (lock) {
            System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }

        // 4. 【重量级锁状态】：多线程激烈竞争（轻量级锁膨胀 → 重量级锁）
        System.out.println("\n---------- 4. 重量级锁状态（多线程激烈竞争） ----------");
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                synchronized (lock) {
                    try {
                        Thread.sleep(5); // 持有锁，加剧竞争
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
        Thread.sleep(200); // 等待锁膨胀
        synchronized (lock) {
            System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }
    }
}