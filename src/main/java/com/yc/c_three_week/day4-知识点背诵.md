# Day4 volatile、JMM、happens-before、指令重排 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、volatile 的本质与两大语义

### 1.1 核心定义

`volatile` 是 Java 最轻量级的同步机制，仅用 1 个关键字实现两个核心语义：

| 语义 | 含义 | 底层机制 |
|---|---|---|
| **保证可见性** | 一个线程修改 volatile 变量，其他线程**立即可见** | 写立即刷主内存 + 读强制从主内存取 |
| **禁止指令重排** | volatile 变量前后的代码不会被重排序 | 内存屏障（Memory Barrier）截断重排 |

### 1.2 volatile 变量读写底层操作

```java
volatile int flag = 0;

// 线程 A 写
flag = 1;
// 底层：CPU 执行完后 → StoreStore 屏障 → 强制刷到主内存 → StoreLoad 屏障

// 线程 B 读
int v = flag;
// 底层：读之前 → LoadLoad 屏障 → 强制从主内存重新读取 → LoadStore 屏障
```

**volatile 写**：JMM 会**立刻**将工作内存的值刷新到主内存。
**volatile 读**：JMM 会**先清空**工作内存的缓存，从主内存重新读取。

### 1.3 volatile 不能保证原子性 ⭐

```java
volatile int count = 0;

// 10 个线程各执行 1000 次 count++，结果 ≠ 10000
// 因为 count++ 分三步：
//   ① 从主内存读取 count 到工作内存（volatile 保证读到最新值）
//   ② 在工作内存中 count + 1（此时可能被打断）
//   ③ 写回主内存（volatile 保证立即刷回）
//
// volatile 只保证 ① 和 ③ 实时，不保证 ② 不被打断
```

**解决方案**：
- `synchronized` 锁住整个过程
- `AtomicInteger.getAndIncrement()` → CAS 原子自增

---

## 二、内存屏障（Memory Barrier）—— volatile 的底层实现

### 2.1 四种内存屏障

| 屏障类型 | 含义 | 作用 |
|---|---|---|
| **LoadLoad** | Load1; **LoadLoad**; Load2 | 保证 Load1 的读在 Load2 之前完成 |
| **StoreStore** | Store1; **StoreStore**; Store2 | 保证 Store1 的写对其他处理器可见后才执行 Store2 |
| **LoadStore** | Load1; **LoadStore**; Store2 | 保证 Load1 的读在 Store2 的写之前完成 |
| **StoreLoad** | Store1; **StoreLoad**; Load2 | **最重屏障**，保证 Store1 对所有处理器可见后才执行 Load2 |

### 2.2 volatile 读写的屏障插入

```
volatile 写：
  StoreStore 屏障  ← 保证 volatile 写之前的普通写全部完成
  volatile 写
  StoreLoad 屏障   ← 保证 volatile 写后的读操作读到最新值（最重的屏障）

volatile 读：
  LoadLoad 屏障    ← 保证 volatile 读之前的读全部完成
  volatile 读
  LoadStore 屏障   ← 保证 volatile 读之后的写不被重排到读前面
```

**口诀**：**写前 StoreStore，写后 StoreLoad；读前 LoadLoad，读后 LoadStore**。

---

## 三、DCL（双重检查锁）单例为什么要 volatile ⭐⭐⭐

### 3.1 完整代码

```java
public class Singleton {
    // volatile 的含义是：instance 的初始化做了指令重排
    // volatile 就像一道墙，把 instance = new Singleton() 锁住，必须按顺序执行
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                 // 第一次检查
            synchronized (Singleton.class) {
                if (instance == null) {         // 第二次检查
                    instance = new Singleton(); // ★ 关键行
                }
            }
        }
        return instance;
    }
}
```

### 3.2 new Singleton() 的致命三步

```
instance = new Singleton();

CPU 层面的三步：
  ① memory = allocate()    分配内存空间
  ② ctorSingleton(memory)  在分配的空间上调用构造器初始化对象
  ③ instance = memory      将 instance 引用指向分配的内存地址

不加 volatile：
  ② 和 ③ 可能被指令重排 → instance 先指向了内存地址，但对象还没初始化
  → 此时线程 B 执行第一个 if (instance == null) → 发现非 null
  → 直接 return instance → 拿到一个未初始化完成的对象 → 灾难！

加 volatile：
  StoreStore 屏障挡在 ①-② 之后 → 保证 ①-② 全部完成后才执行 ③
  → instance 引用被赋值时，对象一定是构造完整的
```

### 3.3 对象半初始化问题图解

```
时间 ────────────────────────────────────────→

线程 A (new Singleton):
  ① 分配内存  ② 初始化对象（构造函数）
                     ↓
                 ③ 赋值引用 → instance = 内存地址
                                   │
不加volatile: ②③可能颠倒 ──────────┘
             → ③先执行：instance 非 null，但对象字段还是默认值！

线程 B (getInstance):
  检查 instance != null → true (但对象还没构造完!)
  → 返回半初始化对象 → 字段全是 null/0 → 使用时报 NPE
```

**volatile 的作用**：禁止 ② 和 ③ 重排序，保证对象**完全初始化后** instance 才被赋值。

---

## 四、volatile 与 synchronized 对比

| 对比维度 | volatile | synchronized |
|---|---|---|
| **本质** | 轻量级，只修饰变量 | 重量级，锁代码块/方法 |
| **原子性** | **不保证** | **保证** |
| **可见性** | **保证** | **保证** |
| **有序性** | **保证**（内存屏障禁止重排） | **保证**（串行执行） |
| **阻塞** | 不阻塞 | 未获锁的线程阻塞 |
| **性能开销** | 极小（仅内存屏障） | 较大（锁升级 + 上下文切换） |
| **适用场景** | 状态标志位 / DCL 单例 | 复合操作（i++、check-then-act） |

---

## 五、JMM 与 volatile 的可见性保证

### 5.1 volatile 写-读模型的 happens-before 关系

```
volatile 变量的 happens-before 规则：
  volatile 写 HB volatile 读

这意味着：
  - volatile 写之前的所有操作 → 对 volatile 读之后的所有操作可见
  - volatile 变量成了一条"可见性分界线"
```

### 5.2 volatile 写-读模式的典型应用

```java
// volatile 标记位：一个线程写，多个线程读
class Task {
    private volatile boolean running = true;

    public void stop() {
        running = false;  // 主线程修改 → 立即可见
    }

    public void run() {
        while (running) {  // 工作线程读取 → 永远是最新值
            // 执行业务
        }
    }
}
// 不用 volatile → 工作线程可能永远看不到 running 被改为 false → 死循环！
```

### 5.3 volatile 不适用的场景

```java
// ❌ volatile 不能用于依赖当前值的场景
volatile int count = 0;
count = count + 1;  // 不是原子操作！读取 → 计算 → 写入 三步之间可能被打断

// ✅ 正确：用 AtomicInteger
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();  // CAS 原子操作
```

---

## 六、指令重排的更多实战场景

### 6.1 构造函数的逸出问题

```java
public class ThisEscape {
    private int value;

    public ThisEscape(EventSource source) {
        source.registerListener(new EventListener() {  // 这里发布了 this！
            public void onEvent(Event e) {
                doSomething(e);  // 可能读到 value = 0（还没初始化完）
            }
        });
        this.value = 42;  // 构造还没完成
    }
}
// 解决：不要在构造函数中发布 this（启动线程 / 注册监听器）
```

### 6.2 双重检查的 volatile 不可省略

```java
// ❌ 没有 volatile 的 DCL —— 在 JDK 1.4 及之前是安全的，JDK 1.5 之后不行
private static Singleton instance;  // 缺少 volatile
// → JIT 编译器的指令重排会导致半初始化对象

// ✅ 正确的 DCL
private static volatile Singleton instance;
```

### 6.3 final 域的重排保护

```java
// final 字段在构造函数中赋值后，JMM 保证：
// 1. 构造函数中对 final 域的写入 → 与对象的引用赋值 → 不会被重排
// 2. 第一次读对象引用 → 与第一次读该 final 域 → 不会被重排
// 所以 final 域可以不需同步地安全访问
```

---

## 七、StoreLoad 屏障为什么最重？

StoreLoad 屏障同时具备其他三个屏障的效果，执行时需要：
1. 确保 Store 已经刷到主内存（等待 Store Buffer 排空）
2. 确保后续 Load 从主内存取（清空 Invalidate Queue）
3. 这在 x86 上需要 **mfence** 指令或 `lock addl $0x0, (%rsp)` 模拟 → 几十个 CPU 周期

**而其他三个屏障在 x86 上几乎零开销**（x86 是强内存模型，LoadLoad/LoadStore/StoreStore 天然保证，只有 StoreLoad 需要显式指令）。

---

## 八、终极背诵总结

1. **volatile 两大语义**：保证可见性（刷主内存） + 禁止指令重排（内存屏障）
2. **volatile 不保证原子性**：i++ 三步中间可被打断 → 用 AtomicInteger
3. **DCL 必须 volatile**：防止 `new` 的初始化与引用赋值重排 → 半初始化对象
4. **四种屏障**：LoadLoad / StoreStore / LoadStore / StoreLoad（最重）
5. **写屏障**：写前 StoreStore → 写完 StoreLoad
6. **读屏障**：读前 LoadLoad → 读完 LoadStore
7. **适用场景**：状态标志位 + DCL 单例 + 单写多读的简单变量
8. **不适用场景**：i++、check-then-act 等依赖旧值的复合操作
