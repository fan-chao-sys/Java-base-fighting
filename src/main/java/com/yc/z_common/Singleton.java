package com.yc.z_common;

public class Singleton {
    // volatile 禁止指令重排
    private static volatile Singleton instance;

    // 私有构造，防止外部 new
    private Singleton() {
        // 防御：防止反射破坏单例
        if (instance != null) {
            throw new RuntimeException("单例对象已存在，禁止创建！");
        }
    }

    // 3. 公开静态获取方法（双重检查锁）
    public static Singleton getInstance() {
        // 第一次检查：不加锁，提高性能
        if (instance == null) {
            // 加锁：保证多线程下只有一个线程创建实例
            synchronized (Singleton.class) {
                // 第二次检查：防止多个线程同时进入第一次检查后重复创建
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}