package com.yc.Xcommon;

import java.util.concurrent.locks.ReentrantLock;

public class Counter {
    // 计数器变量
    private int count = 0;
    // 可重入锁
    private final ReentrantLock lock = new ReentrantLock();

    // 计数自增
    public void increment() {
        lock.lock(); // 加锁
        try {
            count++;
        } finally {
            lock.unlock(); // 最终释放锁，避免死锁
        }
    }

    // 获取计数值
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}