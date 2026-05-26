package com.yc.Xcommon;

public class Singleton {
    // volatile 禁止指令重排
    private static volatile Singleton instance;

    // 私有构造，防止外部 new
    private Singleton() {}

    // 双重检查获取实例
    public static Singleton getInstance() {
        // 第一次判断：无实例直接返回，减少锁竞争
        if (instance == null) {
            synchronized (Singleton.class) {
                // 第二次判断：防止多线程重复创建
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}