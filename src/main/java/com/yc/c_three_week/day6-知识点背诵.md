# Day6 线程池、阻塞队列、ThreadLocal、CompletableFuture 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、线程池 —— 七大核心参数（必背）⭐⭐⭐

### 1.1 ThreadPoolExecutor 构造器

```java
public ThreadPoolExecutor(
    int corePoolSize,        // ① 核心线程数
    int maximumPoolSize,     // ② 最大线程数
    long keepAliveTime,      // ③ 空闲线程存活时间
    TimeUnit unit,           // ④ 时间单位
    BlockingQueue<Runnable> workQueue,  // ⑤ 阻塞队列
    ThreadFactory threadFactory,        // ⑥ 线程工厂
    RejectedExecutionHandler handler    // ⑦ 拒绝策略
)
```

### 1.2 七大参数逐一解释

| 参数 | 含义 | 记忆要点 |
|---|---|---|
| **corePoolSize** | 核心线程数（常驻线程，不会被回收） | IO密集型 vs CPU密集型的计算见下 |
| **maximumPoolSize** | 最大线程数 = 核心线程 + 非核心临时线程 | 队列满了才创建临时线程 |
| **keepAliveTime** | 非核心线程空闲多久后销毁 | 核心线程不收（除非 allowCoreThreadTimeOut=true） |
| **unit** | keepAliveTime 的时间单位 | `TimeUnit.SECONDS` 等 |
| **workQueue** | 存放等待执行任务的阻塞队列 | `LinkedBlockingQueue` / `ArrayBlockingQueue` / `SynchronousQueue` |
| **threadFactory** | 创建线程的工厂 | 可自定义线程名，便于排查 |
| **handler** | 队列满 + 线程满时的拒绝策略 | Abort / CallerRuns / Discard / DiscardOldest |

### 1.3 线程池工作流程（完整链路）

```
提交任务
  │
  ▼
核心线程是否已满？
  ├─ 否 → 创建核心线程执行任务
  │         （即使有其他核心线程空闲，也优先创建新核心线程）
  │
  └─ 是 → 队列是否已满？
            ├─ 否 → 放入阻塞队列等待
            │
            └─ 是 → 线程数是否 < maximumPoolSize？
                      ├─ 是 → 创建非核心临时线程执行
                      │
                      └─ 否 → 执行拒绝策略
```

**关键理解**：并不是先创建线程再放队列，而是**核心线程 → 队列 → 最大线程 → 拒绝**这个顺序。

---

## 二、四种拒绝策略

| 策略 | 行为 | 适用场景 |
|---|---|---|
| **AbortPolicy**（默认） | 抛 `RejectedExecutionException` | 需要感知任务被拒绝的场景 |
| **CallerRunsPolicy** | 由调用线程（提交任务的线程）自己执行该任务 | 可降低提交速度，提供反压 |
| **DiscardPolicy** | 直接丢弃，不抛异常 | 不重要任务（如日志统计） |
| **DiscardOldestPolicy** | 丢弃队列中最早的未执行任务，重新提交当前任务 | 优先处理最新任务 |

---

## 三、线程池参数配置模板

### 3.1 核心线程数计算公式

```
CPU 密集型：
  核心线程数 = CPU 核心数 + 1
  原因：线程一直占 CPU，多了反而上下文切换浪费

IO 密集型：
  核心线程数 = CPU 核心数 × 2
  或：核心线程数 = CPU 核心数 × (1 + 平均 IO 等待时间 / 平均计算时间)
  原因：IO 等待时线程阻塞让出 CPU，可以多建线程提高吞吐
```

### 3.2 为什么不用 Executors 默认工厂？⭐

```java
// ❌ 禁止使用
Executors.newFixedThreadPool(n);     // LinkedBlockingQueue 无界 → 可能 OOM
Executors.newSingleThreadExecutor(); // 同上
Executors.newCachedThreadPool();     // 允许创建 Integer.MAX_VALUE 个线程 → 可能 OOM

// ✅ 推荐：手动 new ThreadPoolExecutor 自定义参数
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    4,                              // 核心线程数
    8,                              // 最大线程数
    60L, TimeUnit.SECONDS,          // 空闲 60s 回收临时线程
    new LinkedBlockingQueue<>(200), // 有界队列
    new ThreadFactory() {           // 自定义线程名
        private int count = 0;
        public Thread newThread(Runnable r) {
            return new Thread(r, "biz-pool-" + (count++));
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // 调用者执行反压
);
```

**Alibaba 开发手册强制**：线程池必须手动创建，不允许用 Executors。

---

## 四、常用阻塞队列对比

| 队列 | 底层 | 容量 | 特点 |
|---|---|---|---|
| **ArrayBlockingQueue** | 数组 | **有界**（必须指定） | FIFO，一把锁存+取（公平/非公平可选） |
| **LinkedBlockingQueue** | 链表 | 可选有界/无界（默认 `Integer.MAX_VALUE`） | FIFO，两把锁（存取分离，吞吐更高） |
| **SynchronousQueue** | 无存储 | 0（不存元素） | 生产一个必须等消费一个，直接交付 |
| **PriorityBlockingQueue** | 二叉堆 | 无界 | 按优先级排序出队 |
| **DelayQueue** | 二叉堆 | 无界 | 延迟到期后才能取出 |

---

## 五、ThreadLocal —— 线程私有变量 ⭐⭐

### 5.1 核心原理

```java
// 每个 Thread 对象内部都有一个 ThreadLocalMap
public class Thread {
    ThreadLocal.ThreadLocalMap threadLocals;  // 存储该线程的所有 ThreadLocal 值
    // ...
}

// ThreadLocal.set() 本质：
public void set(T value) {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);          // 获取当前线程的 ThreadLocalMap
    if (map != null)
        map.set(this, value);                // key = ThreadLocal 对象；value = 值
    else
        createMap(t, value);
}
```

**一句话总结**：`ThreadLocal` 本身不存值，而是以自己为 key，把值存入**当前线程的 ThreadLocalMap** 中。

### 5.2 内存泄漏问题（重要！）

```
Thread 对象
  │
  └─→ ThreadLocalMap
        │
        └─→ Entry（WeakReference<ThreadLocal> key, Object value）
               │
               ├─ key: ThreadLocal ← [弱引用] → GC 回收后 key = null
               │
               └─ value: 业务对象 ← [强引用] → key 没了但 value 还在！

泄漏链路：
  Thread 还活着 → ThreadLocalMap 还活着
  → Entry 还在（虽然 key 被 GC 了，value 还在）
  → value 无法被 GC → 内存泄漏
```

**为什么 key 用弱引用？**
- 为了防止 key（ThreadLocal 对象）本身无法回收
- 但 value 是强引用 → key 被回收后 value 依然存在

**解决方案**：**用完一定要 `remove()`**

```java
// ✅ 正确用法
ThreadLocal<User> local = new ThreadLocal<>();
try {
    local.set(user);
    // 使用...
} finally {
    local.remove();  // 必须 remove！否则可能内存泄漏
}
```

### 5.3 ThreadLocalMap 的哈希冲突解决

- 与 HashMap 的拉链法不同，ThreadLocalMap 使用**线性探测法**（开放地址法）
- 冲突时往后找下一个空槽位
- 在 `get`/`set`/`remove` 过程中，会顺便清理 key==null 的"脏 Entry"（expunge stale entries）

### 5.4 ThreadLocal 典型应用场景

| 场景 | 说明 |
|---|---|
| **数据库连接** | Spring 事务管理器，同一个事务用同一个 Connection |
| **Session 管理** | 每个请求线程保存用户登录信息 |
| **日期格式化** | `SimpleDateFormat` 非线程安全，放 ThreadLocal 里 |
| **链路追踪** | 分布式链路 traceId 透传 |

---

## 六、CompletableFuture 异步编排

### 6.1 核心能力

```java
// ① 异步执行 + 链式回调
CompletableFuture.supplyAsync(() -> 查询订单())      // 异步查询
    .thenApply(order -> 计算金额(order))              // 转换结果
    .thenAccept(amount -> System.out.println(amount))  // 消费结果
    .exceptionally(e -> { log.error(e); return null; }); // 异常处理

// ② 两个任务合并
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");
f1.thenCombine(f2, (s1, s2) -> s1 + " " + s2);  // "Hello World"

// ③ 多任务并行等待
CompletableFuture.allOf(f1, f2, f3).join();  // 等待全部完成
CompletableFuture.anyOf(f1, f2, f3).join();  // 任意一个完成
```

### 6.2 常用方法速查

| 方法 | 作用 | 类比 |
|---|---|---|
| `thenApply(fn)` | 转换结果（有返回值） | Stream.map |
| `thenAccept(consumer)` | 消费结果（无返回值） | Stream.forEach |
| `thenRun(runnable)` | 不依赖前一步结果，执行动作 | — |
| `thenCombine(other, fn)` | 合并两个结果 | — |
| `thenCompose(fn)` | 依赖上一个结果执行异步操作（展平） | Stream.flatMap |
| `exceptionally(fn)` | 异常恢复 | try-catch |
| `allOf(futures)` | 全部完成才继续 | CountDownLatch |
| `anyOf(futures)` | 任意一个完成就继续 | — |

---

## 七、终极背诵总结

1. **线程池7参数**：coreSize → maxSize → keepAlive → TimeUnit → Queue → Factory → Handler
2. **执行流程**：核心线程 → 阻塞队列 → 最大线程 → 拒绝策略
3. **不用 Executors**：无界队列 OOM / 无限线程 OOM → 手动指定有界队列
4. **核心线程数**：CPU密集型 = 核数+1；IO密集型 = 核数×2
5. **4 种拒绝策略**：抛异常 / 调用者执行 / 丢弃 / 丢弃最旧
6. **ThreadLocal 原理**：Thread 的 ThreadLocalMap → key 为弱引用 → 用完必须 remove
7. **内存泄漏链路**：Thread活着 → Map活着 → key被GC但value强引用还在 → 泄漏
8. **CompletableFuture**：异步编排利器，链式回调、多任务组合、异常兜底
