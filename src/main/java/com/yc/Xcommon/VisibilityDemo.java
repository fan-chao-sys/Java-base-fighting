package com.yc.Xcommon;

public class VisibilityDemo {
    // 不加 volatile！！！核心！！！
    private boolean flag = false;

    public void changeFlag() {
        try {
            // 让主线程先跑起来进入循环
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 修改 flag
        flag = true;
        System.out.println(Thread.currentThread().getName() + " 已将 flag 修改为 true");
    }

    public void printFlag() {
        System.out.println("等待 flag 变成 true……");
        
        // 如果没有可见性，这里会一直死循环！
        while (!flag) {
            // 空循环，故意不写任何代码，让JIT极致优化
        }
        
        System.out.println("成功感知到 flag = true，线程退出！");
    }


}