# Day7 周测知识点总结 —— Java 并发编程与 JUC

> 覆盖 Day1~Day6 全部核心知识点，对应"学习任务清单" Day 7 整周复盘要求。

---

## 一、10 道自测题（口述标准答案）

### Q1: 线程安全问题由哪三个要素引起？用什么技术分别解决？

**答：**

**三要素**：
1. **原子性**：一个操作不可再分割（如 i++ 分"读-改-写"三步，中间可被打断）
2. **可见性**：一个线程修改共享变量后，其他线程能立即看到
3. **有序性**：程序按代码书写顺序执行（不会被指令重排打乱）

**解决技术矩阵**：

| | 原子性 | 可见性 | 有序性 |
|---|---|---|---|
| **synchronized** | ✓ | ✓ | ✓ |
| **ReentrantLock** | ✓ | ✓ | ✓ |
| **volatile** | ✗ | ✓ | ✓ |
| **AtomicInteger** | ✓ | ✓ | ✗（单个操作原子，复合操作不行） |
| **final** | — | ✓ | 部分 |

**补充5种避免线程安全的方式**：无共享（ThreadLocal）/ 不可变（final）/ 互斥同步（synchronized/Lock）/ volatile / 原子类。

---

### Q2: synchronized 的锁升级过程（无锁 → 偏向 → 轻量级 → 重量级）？

**答：**

```
无锁 (001)
  │  第一次有线程获取锁
  ▼
偏向锁 (101)  ← CAS 将线程 ID 写入 Mark Word
  │  同一线程重入 → 只检查 ID，无需 CAS（零开销）
  │
  │  有其他线程竞争，到达安全点，撤销偏向
  ▼
轻量级锁 (00)  ← 栈帧创建 Lock Record，CAS 将 Mark Word 替换为指针
  │  竞争线程 CAS 自旋等待
  │
  │  自旋超限（自适应）/ 竞争激烈
  ▼
重量级锁 (10)  ← 创建 Monitor，竞争线程进入 EntryList 阻塞（BLOCKED）
  │  未获得锁的线程被 OS 挂起，等待被唤醒
  ▼
  等待 OS 调度唤醒
```

**本周验证代码（W3D3_Actual）**：使用 jol-core 打印对象头，依次观察无锁→偏向锁→轻量级锁→重量级锁的 Mark Word 变化。

**JDK 15+ 默认关闭偏向锁**：现代应用并发度高，偏向锁撤销成本 > 收益。

---

### Q3: volatile 保证什么？不能保证什么？为什么？

**答：**

| | 是否保证 | 底层机制 |
|---|---|---|
| **可见性** | ✓ 保证 | 写立即刷主内存 + 读强制从主内存取 |
| **有序性** | ✓ 保证 | 内存屏障禁止指令重排 |
| **原子性** | ✗ **不保证** | i++ 分三步（读→改→写），volatile 只保证每步的可见性，不保证三步不被打断 |

**原子性反例**：
```java
volatile int count = 0;
// 10个线程各执行 1000 次 count++ → 结果 < 10000
// 因为 count++ = 读 + 加1 + 写回，三步之间可能被其他线程插入
```

**DCL 单例必须用 volatile**（W3D4_Actual + Singleton 验证）：
```java
instance = new Singleton();  // 三步：分配内存 → 初始化对象 → 赋值引用
// 不加 volatile → ②和③可能重排 → 其他线程拿到"非null但未初始化完成"的对象
```

**可见性验证（W3D2_Actual + VisibilityDemo）**：不加 volatile 的 flag，子线程可能永远看不到主线程的修改 → 死循环。

---

### Q4: AQS 的核心思想是什么？用一句话概括

**答：**

> **AQS = state（同步状态变量） + CLH 变体 FIFO 等待队列 + CAS 操作 state**

- **state**：用 `volatile int state` 表示同步状态（0=未锁，1=已锁，>1=重入次数）
- **CLH 队列**：双向链表，每个 Node 对应一个等待线程，FIFO 保证公平
- **CAS**：无锁修改 state，失败则入队自旋/阻塞

**模板方法模式**：AQS 提供 `acquire()` / `release()` 框架，子类只需实现 `tryAcquire()` / `tryRelease()`。

**基于 AQS 的常见实现**：

| 类 | state 含义 |
|---|---|
| `ReentrantLock` | 0=未锁，≥1=重入次数 |
| `Semaphore` | 剩余许可数 |
| `CountDownLatch` | 未完成计数，减到 0 释放 |
| `ReentrantReadWriteLock` | 高16位=读锁数，低16位=写锁重入数 |

---

### Q5: CAS 有什么问题？ABA 怎么解决？

**答：**

**CAS 三大问题**：

| 问题 | 说明 | 解决方案 |
|---|---|---|
| **ABA 问题** | 值从 A→B→A，CAS 检测不到中间变化 | `AtomicStampedReference`（版本号） |
| **循环开销** | CAS 失败 → 自旋重试 → 消耗 CPU | 竞争激烈时用 `synchronized` 或 `LongAdder` |
| **单变量限制** | 只能保证一个共享变量的原子操作 | 封装成对象用 `AtomicReference` |

**ABA 问题推演**：
```
线程1: 读 value=A，准备 CAS A→C
线程2: CAS A→B → CAS B→A  （值变回了 A！）
线程1: CAS A→C 成功       （不知道中间被修改过）
```

**AtomicStampedReference 解决**：
```java
AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(100, 0);
int stamp = ref.getStamp();
ref.compareAndSet(expectVal, newVal, stamp, stamp + 1);  // 版本号不匹配则失败
```

**LongAdder 优化原理**（本周 W3D5_Actual 中用 Counter 演示 ReentrantLock 计数）：
- `AtomicLong`：所有线程 CAS 竞争一个变量 → 高并发失败率飙升
- `LongAdder`：base + Cell[] 分段累加，写时分到不同 Cell → CAS 冲突大幅降低
- 读时 `sum()` = base + Cell[0] + Cell[1] + ...（不是实时精确值）

---

### Q6: 线程池 7 个参数分别控制什么？

**答：**

```java
new ThreadPoolExecutor(
    corePoolSize,      // ① 核心线程数（常驻，不回收）
    maximumPoolSize,   // ② 最大线程数（核心 + 临时）
    keepAliveTime,     // ③ 非核心线程空闲存活时间
    unit,              // ④ 时间单位
    workQueue,         // ⑤ 阻塞队列（存等待任务）
    threadFactory,     // ⑥ 线程工厂（自定义线程名）
    handler            // ⑦ 拒绝策略（队列满 + 线程满时）
)
```

**执行流程口诀**：**核心线程 → 阻塞队列 → 最大线程 → 拒绝策略**

**4 种拒绝策略**：

| 策略 | 行为 |
|---|---|
| **AbortPolicy**（默认） | 抛 RejectedExecutionException |
| **CallerRunsPolicy** | 由提交任务的线程自己执行（反压） |
| **DiscardPolicy** | 直接丢弃，不抛异常 |
| **DiscardOldestPolicy** | 丢弃队列中最早的任务 |

**本周实战代码（W3D6_Actual + CustomThreadPool）**：`IO密集型: core = CPU核数×2, max = CPU核数×4, ArrayBlockingQueue(128), CallerRunsPolicy`

---

### Q7: 核心线程数怎么估算？

**答：**

```
CPU 密集型：
  核心线程数 = CPU 核数 + 1
  原因：线程一直在计算，多了上下文切换浪费 CPU

IO 密集型：
  核心线程数 = CPU 核数 × 2
  或更精确：核心线程数 = CPU核数 × (1 + 平均IO等待时间 / 平均计算时间)
  原因：IO 等待时线程阻塞，CPU 空闲，可以多建线程提高吞吐

混合型：
  核心线程数 = CPU核数 × (1 + WT/ST)
  WT = 平均等待时间, ST = 平均计算时间
```

**为什么不用 Executors？**
- `newFixedThreadPool` / `newSingleThreadExecutor` → 无界 LinkedBlockingQueue → **OOM**
- `newCachedThreadPool` → max = Integer.MAX_VALUE → 无限创建线程 → **OOM**
- **Alibaba 规范强制**：必须手动 `new ThreadPoolExecutor` 指定有界队列

---

### Q8: ThreadLocal 内存泄漏原因和解决方案？

**答：**

**泄漏链路**：

```
Thread（线程池常驻，一直活着）
  └─→ ThreadLocalMap
        └─→ Entry {
              key: ThreadLocal ← [弱引用] → GC 后 key = null
              value: 业务对象 ← [强引用] → key 没了但 value 还在！
            }
```

**为什么 key 用弱引用？** 防止 ThreadLocal 对象本身无法被 GC。但 value 是强引用 → key 被回收后 value 仍存在，且 Entry 挂在 Thread 整个生命周期上 → **内存泄漏**。

**解决方案**：**用完必须 remove()**

```java
ThreadLocal<User> local = new ThreadLocal<>();
try {
    local.set(user);
    // 使用...
} finally {
    local.remove();  // ← 必须！线程池场景尤其重要
}
```

**本周验证代码（W3D6_Actual）**：
- Thread-1：执行 remove() → 复用后值为 null
- Thread-2：不执行 remove() → 复用后残留旧值 → **验证了泄漏和脏数据问题**

---

### Q9: synchronized 和 ReentrantLock 的 5 个区别？

**答：**

| 对比维度 | synchronized | ReentrantLock |
|---|---|---|
| **本质** | JVM 关键字 | JDK API（`java.util.concurrent.locks`） |
| **锁释放** | **自动释放**（代码块结束 / 异常） | **手动释放**（必须 finally unlock，否则死锁） |
| **可中断** | 不可中断，等不到锁就一直等 | `lockInterruptibly()` 可响应中断 |
| **公平性** | 非公平（不保证先来先得） | 可选**公平/非公平**（构造器传参，默认非公平） |
| **条件唤醒** | 只能 `wait/notify` 一个条件 | **多个 Condition**，精确唤醒不同等待线程组 |
| **尝试获取** | 不支持（等不到就堵死） | `tryLock(timeout)` 超时放弃 |

**本周验证代码（W3D5_Actual + Counter + ProducerConsumerDemo）**：
- Counter：用 `ReentrantLock` 实现线程安全计数器，演示 lock/unlock 范式
- ProducerConsumerDemo：用 `Lock + Condition(notFull/notEmpty)` 实现生产者消费者，精确唤醒

---

### Q10: 怎么排查死锁？你用过哪些工具？

**答：**

**排查步骤**：

```
1. jps -l                             → 找到 Java 进程 PID
2. jstack <pid> > thread.txt          → dump 线程栈
   → 搜索 "deadlock" 或 "BLOCKED"
   → jstack 会直接打印 Found deadlock + 死锁线程信息
3. 分析死锁日志：
   - Found one Java-level deadlock
   - Thread-0: waiting to lock <0x...> (held by Thread-1)
   - Thread-1: waiting to lock <0x...> (held by Thread-0)
4. 根据类名+行号定位死锁代码
```

**其他排查工具**：
- `jconsole` / `jvisualvm`：GUI 工具，检测死锁一键操作
- `jcmd <pid> Thread.print`：JDK 9+ 替代 jstack
- Arthas：`thread -b` 直接找出死锁/阻塞线程
- JProfiler / MAT：专业性能分析

**预防死锁**：
- 固定加锁顺序（所有线程按相同顺序获取锁）
- `tryLock(timeout)` 超时放弃而非无限等待
- 减少锁的持有时间、降低锁粒度

---

## 二、JUC 高频题笔记

### 2.1 并发编程核心骨架

```
线程创建 4 方式 → start 调 run → 6 种状态流转
       │
       ▼
线程安全问题三要素：原子性 + 可见性 + 有序性
       │
       ├──→ synchronized：锁升级（偏向→轻量→重量）
       ├──→ volatile：可见性 + 有序性，不保证原子性
       ├──→ CAS + AtomicXxx：无锁原子操作
       └──→ AQS 框架：ReentrantLock / Semaphore / CountDownLatch
       │
       ▼
线程池：7 参数 + 4 拒绝策略 + IO/CPU 核数公式
       │
       ▼
ThreadLocal：Thread 私有的 Map，用后必 remove
       │
       ▼
CompletableFuture：异步编排，链式回调
```

### 2.2 锁升级路径速记

```
无锁(001) → 偏向锁(101，CAS写线程ID) → 轻量级锁(00，CAS自旋)
   → 重量级锁(10，OS mutex阻塞)
   
Mark Word 最低3位 = 锁状态标志：
  001=无锁  101=偏向锁  00=轻量级锁  10=重量级锁  11=GC标记
```

### 2.3 AQS 实现速查表

| 实现类 | state 语义 | 核心方法 |
|---|---|---|
| ReentrantLock | 0=未锁, ≥1=重入次数 | tryAcquire / tryRelease |
| Semaphore | 剩余许可数 | tryAcquireShared |
| CountDownLatch | 倒计数，减到0释放 | — |
| ReentrantReadWriteLock | 高16位读锁数, 低16位写锁重入 | tryAcquire / tryAcquireShared |

### 2.4 volatile 使用场景矩阵

| 场景 | 能用 volatile？ |
|---|---|
| 状态标志位（stop flag） | ✓ 完美 |
| DCL 单例 | ✓ 必须（禁止重排） |
| i++ 计数 | ✗ 不保证原子性 |
| check-then-act | ✗ 复合操作 |
| 多写多读 | ✗ 需要 CAS 或锁 |

---

## 三、线程池参数配置模板

### 3.1 标准模板

```java
// ========== IO 密集型（推荐） ==========
int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 4;
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(128);

// ========== CPU 密集型 ==========
int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
int maximumPoolSize = Runtime.getRuntime().availableProcessors() + 1;
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);

// ========== 混合型 ==========
int corePoolSize = Runtime.getRuntime().availableProcessors() * (1 + avgWaitTime / avgComputeTime);

// 生产标准线程池
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    corePoolSize, maximumPoolSize,
    60L, TimeUnit.SECONDS,
    queue,
    new ThreadFactory() {
        private final AtomicInteger n = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "biz-pool-" + n.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // 反压
);
```

### 3.2 参数调优三原则

1. **Xms == Xmx**：堆内存初始=最大，避免动态扩缩
2. **有界队列必须配**：不能用无界 LinkedBlockingQueue（OOM 隐患）
3. **CallerRunsPolicy 反压**：比 AbortPolicy 更安全，不会丢任务

---

## 四、并发常见故障排查清单

| 故障现象 | 可能原因 | 排查工具/方法 |
|---|---|---|
| CPU 100% | 死循环 / GC 频繁 / CAS 自旋 / 死锁 | `top -Hp` + `jstack` 找 RUNNABLE 线程 |
| 线程 BLOCKED 大量堆积 | 死锁 / 锁粒度过大 / DB 连接池耗尽 | `jstack` 看 BLOCKED 状态 + 死锁检测 |
| OOM: unable to create new native thread | 线程数超 OS 限制 | `jstack` 统计线程数 → 线程池 max 设太大或没设上限 |
| OOM: Java heap space | 堆不够 / 内存泄漏 | `jmap -dump` → MAT 分析最大对象 |
| 响应变慢但不报错 | 线程池队列堆积 | `pool.getQueue().size()` + `pool.getActiveCount()` |
| ThreadLocal 脏数据 | 未 remove | 线程池线程复用时读到上一个请求的值 → 检查 remove 调用 |
| 数据不一致 | 未加同步 / volatile 误用 | Review 共享变量读写是否有 synchronized / Lock / Atomic 保护 |

### 死锁快速定位命令

```bash
# 1. 找 Java 进程
jps -l

# 2. dump 线程栈，搜索 deadlock
jstack <pid> | grep -A 50 "deadlock"

# 3. Arthas 一键检测
thread -b

# 4. jconsole GUI → 线程 Tab → "检测死锁"
```

---

## 五、本周重点代码清单

| 序号 | 代码 | 文件 | 关键点 |
|---|---|---|---|
| 1 | 4种方式创建线程 | W3D1_Actual + MyThread/MyRunnable/MyCallable | Thread / Runnable / Callable+FutureTask / 线程池 |
| 2 | wait/notify 交替打印奇偶数 | W3D1_Actual + PrintNum | synchronized + wait + notify，while 检查 |
| 3 | 可见性验证 | W3D2_Actual + VisibilityDemo | 不加 volatile → 死循环 |
| 4 | synchronized 锁升级 | W3D3_Actual | jol-core 打印 Mark Word 4 态变化 |
| 5 | volatile 可见性 + DCL | W3D4_Actual + Singleton | 不加 volatile flag 死循环 + DCL 防重排 |
| 6 | ReentrantLock 计数器 | W3D5_Actual + Counter | lock/try-finally-unlock 范式 |
| 7 | Lock+Condition 生产者消费者 | W3D5_Actual + ProducerConsumerDemo | notFull/notEmpty 精确唤醒 |
| 8 | 自定义线程池 | W3D6_Actual + CustomThreadPool | 7参数 + CallerRunsPolicy |
| 9 | ThreadLocal remove 验证 | W3D6_Actual | remove 前后对比 + 线程复用脏数据 |

---

## 六、速记口诀

1. **线程安全三要素**：原子操作不可分、可见修改变量知、有序执行不重排
2. **锁升级**：无锁偏向轻量重，Mark Word 三位变
3. **volatile 双语义**：可见性刷内存，有序性加屏障，原子性——不保！
4. **AQS 一句话**：state 变量 + CLH 队列 + CAS 操作，模板方法给子类
5. **CAS 三问题**：ABA 加版本、自旋费 CPU、只保单个变量
6. **线程池流程**：核心满进队列，队列满扩容最大，最大满走拒绝
7. **拒绝策略**：Abort 抛异常、CallerRuns 反压、Discard 丢弃、DiscardOld 丢旧的
8. **IO vs CPU 核数**：IO 密集型核数乘 2，CPU 密集型核数加 1
9. **ThreadLocal 必 remove**：弱引用 key 被 GC，强引用 value 挂线程，用完不 remove 必泄漏
10. **synchronized vs ReentrantLock**：自动释放 vs 手动释放、不可中断 vs 可中断、单条件 vs 多 Condition、非公平 vs 可选公平
