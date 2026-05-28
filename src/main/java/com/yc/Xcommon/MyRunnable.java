package com.yc.Xcommon;

// 1. 实现Runnable接口
public class MyRunnable implements Runnable {
    // 2. 重写run方法
    @Override
    public void run() {
        System.out.println("线程2：实现Runnable接口 -> 线程执行：" + Thread.currentThread().getName());
    }
}
