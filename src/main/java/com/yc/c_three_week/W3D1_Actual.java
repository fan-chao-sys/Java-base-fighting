import com.yc.z_common.MyCallable;
import com.yc.z_common.MyRunnable;
import com.yc.z_common.MyThread;
import com.yc.z_common.PrintNum;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class W3D1_Actual {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 1. 继承Thread类
        MyThread thread = new MyThread();
        // 启动线程（必须调用start()）
        thread.start();
        System.out.println("主线程：" + Thread.currentThread().getName());

        // 2. 实现Runnable接口
        // 创建任务对象
        MyRunnable runnable = new MyRunnable();
        // 把任务交给Thread线程
        Thread thread2 = new Thread(runnable);
        // 启动线程
        thread2.start();

        // 3. 实现Callable接口，指定返回值类型
        MyCallable callable = new MyCallable();
        // 用FutureTask包装（适配Thread，因为Thread只接受Runnable）
        FutureTask<Integer> futureTask = new FutureTask<>(callable);
        // 交给Thread启动
        Thread thread3 = new Thread(futureTask);
        thread3.start();
        // 获取线程返回结果（阻塞等待）
        int result = futureTask.get();
        System.out.println("线程返回结果：" + result);


        // 4. 创建线程池（固定2个线程） 使用 线程池（ExecutorService）
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        // 提交任务（Runnable/Callable都支持）
        threadPool.submit(() -> {
            System.out.println("线程4：线程池执行 -> " + Thread.currentThread().getName());
        });
        // 关闭线程池（生产环境根据需求管理）
        threadPool.shutdown();


        // 写两个线程交替打印奇偶数（用 wait/notify 实现）
        PrintNum printNum = new PrintNum();
        // 线程1：打印奇数
        new Thread(() -> printNum.printOdd(), "奇数线程").start();
        // 线程2：打印偶数
        new Thread(() -> printNum.printEven(), "偶数线程").start();
    }
}