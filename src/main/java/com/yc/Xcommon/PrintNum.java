package com.yc.Xcommon;

public class PrintNum {
    // 共享数字，从1开始打印
    private int count = 1;
    // 最大打印到多少
    private final int MAX = 10;

    // 打印奇数
    public synchronized void printOdd() {
        while (count <= MAX) {
            // 不是奇数 → 等待
            if (count % 2 != 1) {
                try {
                    wait(); // 释放锁，进入等待
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 是奇数 → 打印
            System.out.println(Thread.currentThread().getName() + "：" + count);
            count++;
            notify(); // 唤醒另一个线程
        }
    }

    // 打印偶数
    public synchronized void printEven() {
        while (count <= MAX) {
            // 不是偶数 → 等待
            if (count % 2 != 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 是偶数 → 打印
            System.out.println(Thread.currentThread().getName() + "：" + count);
            count++;
            notify(); // 唤醒另一个线程
        }
    }
}