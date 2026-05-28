import java.util.ArrayList;
import java.util.List;

public class W4D6_Actual {

    // 定义两个锁
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    public static void main(String[] args) {
        // 在本地构造一次 OOM（不断往 List 里加对象），用 -XX:+HeapDumpOnOutOfMemoryError 生成 dump，用 MAT 分析

        // 1.用于保存强引用，保证对象不被 GC 回收
        List<Object> list = new ArrayList<>();
        // 无限循环创建对象，最终撑爆堆内存
        while (true) {
            // 每次创建 1MB 大对象，加速 OOM
            list.add(new byte[1024 * 1024]);
        }

        /**
         * 2.JVM 启动参数
         *  # 固定小堆内存，方便快速 OOM
         *  -Xms100m
         *  -Xmx100m
         *
         *  # 发生 OOM 时自动导出堆快照（核心参数）
         *  -XX:+HeapDumpOnOutOfMemoryError
         *
         *  # 快照文件保存路径（自动生成）
         *  # 不用手动指定，默认在项目目录下：java_pidxxx.hprof
         */

        /***
         *
         *  MAT 分析思路（记住这 3 步）
         *   1.打开 MAT → Load dump 文件
         *   2.点 Leak Suspects Report（泄漏嫌疑报告）
         *   3.看 Problem Suspect 1 → 直接定位：
         *      哪个类占用内存最大
         *      哪个方法、哪个集合持有对象
         *      得出结论：ArrayList 无限添加导致内存泄漏
         *
         */


        // 用 jstack 模拟排查一次死锁（写死锁代码，用 jstack 定位）
        // 线程 1：先拿 lock1，再尝试拿 lock2
//        new Thread(() -> {
//            synchronized (lock1) {
//                System.out.println("线程1 持有 lock1");
//                try { Thread.sleep(100); } catch (Exception ignored) {}
//
//                // 等待 lock2 → 造成死锁
//                synchronized (lock2) {
//                    System.out.println("线程1 持有 lock1 + lock2");
//                }
//            }
//        }, "Thread-1").start();

        // 线程 2：先拿 lock2，再尝试拿 lock1
//        new Thread(() -> {
//            synchronized (lock2) {
//                System.out.println("线程2 持有 lock2");
//                try { Thread.sleep(100); } catch (Exception ignored) {}
//
//                // 等待 lock1 → 造成死锁
//                synchronized (lock1) {
//                    System.out.println("线程2 持有 lock2 + lock1");
//                }
//            }
//        }, "Thread-2").start();


    }
}
