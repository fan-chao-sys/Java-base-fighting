package com.yc.c_three_week;

public class W3D4_Actual {

    // 未加 volatile
    private static boolean flag = true;

    public static void main(String[] args) throws InterruptedException {
        // 子线程：循环监听 flag
        new Thread(() -> {
            while (flag) {
                // 空循环，模拟业务逻辑
            }
            System.out.println("子线程感知到 flag 变为 false，循环结束");
        }, "子线程").start();

        // 主线程休眠 1 秒，保证子线程先启动
        Thread.sleep(1000);
        // 主线程修改 flag
        flag = false;
        System.out.println("主线程已将 flag 修改为 false");
    }
}