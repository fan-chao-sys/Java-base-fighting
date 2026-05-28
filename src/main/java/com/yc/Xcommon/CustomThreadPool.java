package com.yc.Xcommon;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool {

    /**
     * 自定义 生产环境标准线程池
     */
    public static ThreadPoolExecutor createThreadPool() {
        // 1. 核心线程数：CPU 核心数（计算密集型）| CPU核心数*2（IO密集型）
        // 我们配成 IO密集型（最常用，如接口调用、DB、Redis、文件）
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;

        // 2. 最大线程数：根据业务压力，一般是核心线程数 * 3～5
        // 既保证压力大时能扩容，又不会无限创建导致OOM
        int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 4;

        // 3. 非核心线程空闲存活时间：60秒（空闲超过60秒自动销毁）
        long keepAliveTime = 60L;

        // 4. 时间单位
        TimeUnit unit = TimeUnit.SECONDS;

        // 5. 阻塞队列：有界队列 ArrayBlockingQueue（必用！防止OOM）
        // 容量 100～200 是生产常用值，太大队列堆积，太小频繁扩容
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(128);

        // 6. 线程工厂：自定义命名（方便排查问题）
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNum = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("custom-thread-" + threadNum.getAndIncrement());
                // 设置非守护线程，保证任务执行完
                thread.setDaemon(false);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };

        // 7. 拒绝策略：CallerRunsPolicy（由调用线程自己执行，保证任务不丢失）
        // 生产环境最推荐，比抛异常/丢弃更安全
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        // 创建线程池
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );
    }
}