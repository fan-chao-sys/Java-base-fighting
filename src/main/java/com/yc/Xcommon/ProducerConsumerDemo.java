package com.yc.Xcommon;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 生产者消费者模型
 * 基于 ReentrantLock + Condition 实现（等待/唤醒机制）
 */
public class ProducerConsumerDemo {
    // 队列最大容量
    private static final int MAX_SIZE = 5;
    // 消息队列（缓冲区）
    private final Queue<Integer> queue = new LinkedList<>();

    // 锁
    private final ReentrantLock lock = new ReentrantLock();
    // 队列不为满的条件（生产者用）
    private final Condition notFull = lock.newCondition();
    // 队列不为空的条件（消费者用）
    private final Condition notEmpty = lock.newCondition();

    // ===================== 生产者方法 =====================
    public void produce(int num) throws InterruptedException {
        lock.lock(); // 加锁
        try {
            // 队列满了 → 生产者等待
            while (queue.size() == MAX_SIZE) {
                System.out.println(Thread.currentThread().getName() + " 队列已满，等待...");
                notFull.await(); // 等待“队列不满”的信号
            }

            // 生产数据
            queue.offer(num);
            System.out.println(Thread.currentThread().getName() + " 生产：" + num + "，队列长度：" + queue.size());

            // 唤醒消费者：队列现在不为空了
            notEmpty.signal();
        } finally {
            lock.unlock(); // 必须释放锁
        }
    }

    // ===================== 消费者方法 =====================
    public void consume() throws InterruptedException {
        lock.lock();
        try {
            // 队列空了 → 消费者等待
            while (queue.isEmpty()) {
                System.out.println(Thread.currentThread().getName() + " 队列已空，等待...");
                notEmpty.await(); // 等待“队列不为空”的信号
            }

            // 消费数据
            int num = queue.poll();
            System.out.println(Thread.currentThread().getName() + " 消费：" + num + "，队列长度：" + queue.size());

            // 唤醒生产者：队列现在不满了
            notFull.signal();
        } finally {
            lock.unlock();
        }
    }
}