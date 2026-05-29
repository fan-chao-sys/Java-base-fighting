package com.yc.z_common;

// 1. 继承Thread类
public class MyThread extends Thread {
    // 2. 重写run方法，编写线程任务
    @Override
    public void run() {
        System.out.println("线程1：继承Thread类 -> 线程执行：" + Thread.currentThread().getName());
    }
}