package com.yc.Xcommon;

import java.util.concurrent.Callable;

// 1. 实现Callable接口，指定返回值类型
public class MyCallable implements Callable<Integer> {
    // 2. 重写call方法（有返回值，可抛异常）
    @Override
    public Integer call() throws Exception {
        System.out.println("线程3：实现Callable接口 -> 线程执行：" + Thread.currentThread().getName());
        return 100; // 返回执行结果
    }
}