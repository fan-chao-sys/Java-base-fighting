package com.yc.c_three_week;

import java.util.concurrent.ExecutorService;

import static com.yc.Xcommon.CustomThreadPool.createThreadPool;

public class W3D6_Actual {

    // 1. 定义 ThreadLocal 变量，存储字符串
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();


    public static void main(String[] args) {

        // 自定义一个线程池，配置合理的参数，写注释说明为什么这样配
        ExecutorService threadPool = createThreadPool();
        for (int i = 0; i < 20; i++) {
            int taskNum = i;
            threadPool.execute(() -> {
                System.out.println(Thread.currentThread().getName() + " 执行任务：" + taskNum);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        threadPool.shutdown();


        // 写一个 ThreadLocal 使用示例，验证 remove 前后的行为
        // 线程1：使用后调用 remove()
        new Thread(() -> {
            THREAD_LOCAL.set("线程1的数据");
            System.out.println(Thread.currentThread().getName() + " 获取值：" + THREAD_LOCAL.get());

            // 执行 remove
            THREAD_LOCAL.remove();
            System.out.println(Thread.currentThread().getName() + " remove 后获取值：" + THREAD_LOCAL.get());
        }, "Thread-1").start();

        // 线程2：使用后不调用 remove()
        new Thread(() -> {
            THREAD_LOCAL.set("线程2的数据");
            System.out.println(Thread.currentThread().getName() + " 获取值：" + THREAD_LOCAL.get());
            // 不执行 remove
            System.out.println(Thread.currentThread().getName() + " 未 remove，再次获取值：" + THREAD_LOCAL.get());
        }, "Thread-2").start();

        // 模拟线程复用场景（线程池场景，核心验证点）
        System.out.println("===== 模拟线程池复用线程，验证残留值 =====");
        // 复用上面两个线程（简化模拟：新建同名逻辑线程）
        new Thread(() -> {
            // 该线程对应之前 Thread-1（执行过 remove）
            System.out.println("复用Thread-1（已remove）：" + THREAD_LOCAL.get());
        }, "Thread-1").start();

        new Thread(() -> {
            // 该线程对应之前 Thread-2（未执行 remove）
            System.out.println("复用Thread-2（未remove）：" + THREAD_LOCAL.get());
        }, "Thread-2").start();
    }
}