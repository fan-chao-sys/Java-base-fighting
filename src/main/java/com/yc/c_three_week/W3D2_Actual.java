package com.yc.c_three_week;

import com.yc.Xcommon.VisibilityDemo;

public class W3D2_Actual {

    // 写一段代码演示"可见性"问题：一个线程修改 flag，另一个线程看不到（不加 volatile 的情况）
    public static void main(String[] args) {
        VisibilityDemo demo = new VisibilityDemo();
        // 线程1：负责打印，死循环检测flag
        new Thread(demo::printFlag, "线程1").start();
        // 线程2：1秒后修改flag
        new Thread(demo::changeFlag, "线程2").start();
    }
}