# Day1 线程与进程、线程创建、线程状态 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、进程与线程的本质区别

### 1.1 核心定义

| | 进程（Process） | 线程（Thread） |
|---|---|---|
| **定义** | 操作系统**资源分配**的最小单位 | CPU **调度执行**的最小单位 |
| **资源** | 独立的内存空间、文件句柄、堆 | 共享进程的内存空间，独立栈+PC |
| **通信** | IPC（管道/消息队列/共享内存/Socket） | 直接读写共享变量（但需同步） |
| **创建开销** | 大（复制页表、分配内存） | 小（只分配栈+寄存器） |
| **隔离性** | 强，一个进程崩溃不影响其他 | 弱，一个线程崩溃可能带崩整个进程 |

### 1.2 底层关系

```
一个进程至少包含一个线程（主线程 main）
进程 = 资源容器
线程 = 执行流，是进程中真正干活的东西

Java 程序：
  JVM 进程启动 → 创建 main 线程 → GC 线程 / Finalizer 线程等后台线程
```

---

## 二、4 种线程创建方式（必背）

### 2.1 方式对比

| 方式 | 特点 | 适用场景 |
|---|---|---|
| **继承 Thread** | 重写 `run()`，直接 new 后 `start()` | 简单任务，但 Java 单继承限制大 |
| **实现 Runnable** | 实现 `run()`，传入 Thread 构造器 | 无返回值，解耦任务和线程 |
| **实现 Callable** | 实现 `call()`，**有返回值 + 可抛异常** | 需要返回结果，配合 FutureTask/线程池 |
| **线程池** | 复用线程，管理生命周期 | **实际开发唯一推荐方式** |

### 2.2 核心代码示例

```java
// 1. 继承 Thread
class MyThread extends Thread {
    public void run() { System.out.println("Thread 方式"); }
}
new MyThread().start();

// 2. 实现 Runnable（推荐优于继承 Thread）
class MyRunnable implements Runnable {
    public void run() { System.out.println("Runnable 方式"); }
}
new Thread(new MyRunnable()).start();

// 3. Callable + FutureTask
class MyCallable implements Callable<String> {
    public String call() throws Exception { return "结果"; }
}
FutureTask<String> task = new FutureTask<>(new MyCallable());
new Thread(task).start();
String result = task.get();  // 阻塞获取返回值

// 4. 线程池（实际开发唯一推荐）
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.execute(() -> System.out.println("线程池方式"));        // Runnable
Future<String> future = pool.submit(() -> "有返回值");       // Callable
pool.shutdown();
```

### 2.3 Runnable vs Callable 核心区别

| | Runnable | Callable |
|---|---|---|
| 方法 | `void run()` | `V call() throws Exception` |
| 返回值 | 无 | **有**（泛型指定） |
| 异常 | 不能抛受检异常 | **可以抛受检异常** |
| 配合 | `new Thread()` / `execute()` | `FutureTask` / `submit()` |

---

## 三、start() vs run() —— 高频易错点 ⭐

```java
Thread t = new Thread(() -> System.out.println("执行"));
t.run();   // ❌ 只是普通方法调用，在当前线程（main）执行，不创建新线程
t.start(); // ✅ 创建新线程，由 JVM 调用 run()，新线程执行
```

**底层原理**：
- `start()` 调用 native 方法 → JVM 创建新线程 → 线程获得 CPU 时间片后自动回调 `run()`
- `run()` 就是一个普通方法，谁来调用就在哪个线程执行

---

## 四、Java 线程 6 种状态（Thread.State 枚举）

### 4.1 状态流转全图

```
                    获取锁
              ┌──────────────┐
              ▼              │
  NEW ─start→ RUNNABLE ─────BLOCKED
              │   │  ▲
              │   │  │ 获得锁
     sleep(n) │   │  │
     wait(n)  │   │  wait()→释放锁
     join(n)  │   │  park()
              ▼   ▼  │
        TIMED_WAITING ─────── WAITING
              │                  │
              │ sleep到/wait到    │ notify/notifyAll
              │ join完/park到     │ unpark()
              ▼                  ▼
            TERMINATED（线程执行完毕 / 异常退出 / stop）
```

### 4.2 六种状态底层含义

| 状态 | 含义 | 进入方式 | 退出方式 |
|---|---|---|---|
| **NEW** | 线程对象已创建，未调用 `start()` | `new Thread()` | `start()` |
| **RUNNABLE** | 就绪或正在运行（JVM 统一，实际含 ready + running 两种 OS 状态） | `start()` / 从等待获取 CPU | 时间片到 / yield |
| **BLOCKED** | 等待获取监视器锁（synchronized） | 竞争 `synchronized` 失败 | 获取到锁 |
| **WAITING** | 无限期等待其他线程唤醒 | `wait()` / `join()` / `park()` | `notify()` / `notifyAll()` / `unpark()` |
| **TIMED_WAITING** | 限时等待，超时自动唤醒 | `sleep(n)` / `wait(n)` / `join(n)` | 时间到 / notify |
| **TERMINATED** | 线程执行完毕 | `run()` 正常结束 / 异常退出 | 不可恢复 |

### 4.3 核心易混淆点

> **BLOCKED vs WAITING 的本质区别**：
> - BLOCKED：等着**进入** `synchronized` 代码块（等别人释放锁才能进）
> - WAITING：已经**持有锁**，主动调用 `wait()` 放弃锁等待通知

---

## 五、wait / notify / notifyAll 底层机制 ⭐⭐

### 5.1 核心规则

```java
// 三个方法都是 Object 的方法（不是 Thread 的方法）
// 必须在 synchronized 块内调用，否则抛 IllegalMonitorStateException

synchronized (obj) {
    obj.wait();      // 释放锁 + 阻塞等待
    // 被唤醒后，重新竞争锁，拿到锁后从此处继续
}

synchronized (obj) {
    obj.notify();    // 随机唤醒一个等待线程（不释放锁）
    obj.notifyAll(); // 唤醒所有等待线程
}
```

### 5.2 底层原理

1. **wait() 三步曲**：释放锁 → 进入 WaitSet 阻塞 → 被唤醒后重新竞争锁
2. **notify() 注意**：调用 notify 后**当前线程继续持锁**，必须等 sync 块结束才释放
3. **notifyAll() vs notify()**：notify 只唤醒一个（随机），notifyAll 唤醒全部 → 被唤醒的线程一起竞争锁

### 5.3 为什么 wait/notify 定义在 Object 里而不是 Thread？

- 锁是**对象级别**的（Monitor），任意对象都可以作为锁
- wait/notify 是**锁上的操作**，所以属于 Object
- 如果放在 Thread 里，只能在 Thread 对象上 wait，不合理

### 5.4 sleep vs wait 对比

| | sleep(n) | wait(n) |
|---|---|---|
| 所属 | **Thread** 静态方法 | **Object** 实例方法 |
| 锁 | **不释放锁** | **释放锁** |
| 调用位置 | 任何地方 | 必须在 synchronized 内 |
| 唤醒 | 时间到自动醒 | notify / 时间到 |
| 线程状态 | TIMED_WAITING | TIMED_WAITING / WAITING |

---

## 六、高频易错点

### 1. 线程不能重复 start
- 一个线程对象只能调用一次 `start()`，再次调用抛 `IllegalThreadStateException`
- 线程状态变成 TERMINATED 后不可复用

### 2. 主线程结束 ≠ JVM 退出
- 主线程结束时，如果还有**非守护线程**在运行，JVM 不会退出
- 守护线程（daemon thread）随 JVM 退出而终止

### 3. stop() / suspend() / resume() 已过时
- `stop()` 会立即释放所有锁，可能让被锁保护的对象处于不一致状态
- 替代：用 `interrupt()` 发中断信号 + `isInterrupted()` 检查

---

## 七、终极背诵总结

1. **进程 vs 线程**：进程管资源，线程管执行；共享进程资源，独立栈和 PC
2. **4 种创建方式**：Thread / Runnable / Callable / 线程池；开发只用线程池
3. **start vs run**：start 开新线程回调 run，run 是当前线程普通调用
4. **6 种状态**：NEW → RUNNABLE → { BLOCKED | WAITING | TIMED_WAITING } → TERMINATED
5. **wait/notify 三要素**：synchronized 内调用 + 定义在 Object + 必须由同一把锁调用
6. **sleep vs wait**：sleep 不释放锁（Thread 方法），wait 释放锁（Object 方法）
