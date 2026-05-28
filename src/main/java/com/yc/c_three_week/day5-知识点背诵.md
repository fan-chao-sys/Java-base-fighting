# Day5 ReentrantLock、AQS、CAS、原子类 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、AQS（AbstractQueuedSynchronizer）—— JUC 基石

### 1.1 AQS 是什么

AQS 是 JUC 包中**所有锁和同步器的基类**，提供了一套基于 **FIFO 等待队列** + **int state 状态变量** 的同步框架。

**核心要素**：
```
AQS = state（同步状态） + CLH 变体队列（等待线程） + CAS（操作 state）

state = 0  → 未锁定，自由状态
state = 1  → 已锁定
state > 1  → 重入次数（ReentrantLock 可重入的体现）
```

### 1.2 CLH 队列结构

```
         head                        tail
          │                            │
          ▼                            ▼
    ┌─────────┐   ┌─────────┐   ┌─────────┐
    │  Node   │──→│  Node   │──→│  Node   │──→ null
    │（哨兵）  │←──│ prev    │←──│ prev    │
    │waitStatus│   │waitStatus│   │waitStatus│
    └─────────┘   └─────────┘   └─────────┘
      傀儡节点         等待线程1       等待线程2
```

- **双向链表**，每个节点对应一个等待线程
- **head** 是傀儡哨兵节点（已获取到锁的线程占位）
- **tail** 指向最后一个等待节点
- 新线程入队：CAS 设置 tail 指针
- 获取锁：head 的后继节点通过自旋/CAS 尝试获取

### 1.3 AQS 的模板方法模式

AQS 定义了 `acquire` / `release` 模板，子类只需实现 `tryAcquire` / `tryRelease`：

```java
// AQS 提供的模板方法
public final void acquire(int arg) {
    if (!tryAcquire(arg))           // ① 子类实现：尝试获取锁
        acquireQueued(              // ② 失败 → 入队 + 自旋/阻塞
            addWaiter(Node.EXCLUSIVE), arg);
}

public final boolean release(int arg) {
    if (tryRelease(arg)) {          // ① 子类实现：尝试释放
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);     // ② 唤醒后继节点
        return true;
    }
    return false;
}
```

### 1.4 基于 AQS 的常见实现

| 类 | state 含义 | 特点 |
|---|---|---|
| **ReentrantLock** | 0=未锁，≥1=重入次数 | 可重入互斥锁 |
| **Semaphore** | state = 剩余许可数 | 控制并发访问数量 |
| **CountDownLatch** | state = 未完成计数 | 一次性，减到 0 唤醒 |
| **ReentrantReadWriteLock** | 高16位=读锁，低16位=写锁 | 读写分离 |
| **CyclicBarrier** | 不基于 AQS（用 ReentrantLock+Condition） | 可复用屏障 |

---

## 二、ReentrantLock 深度解析

### 2.1 核心特性

| 特性 | 说明 |
|---|---|
| **可重入** | 同一线程可多次获取同一把锁，state 记录重入次数 |
| **可中断** | `lockInterruptibly()` 等锁时可响应中断 |
| **可超时** | `tryLock(timeout, unit)` 限时等待 |
| **公平/非公平** | 公平锁先检查队列，非公平锁直接 CAS 抢 |
| **多 Condition** | 可创建多个条件队列，精确唤醒 |

### 2.2 公平锁 vs 非公平锁

```java
// 公平锁：先检查队列中有无等待者
final boolean tryAcquire(int acquires) {
    if (state == 0) {
        if (!hasQueuedPredecessors() &&  // ← 关键：检查是否有前驱节点
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // ... 重入逻辑
}

// 非公平锁：直接 CAS 抢，不检查队列
final boolean tryAcquire(int acquires) {
    if (state == 0) {
        if (compareAndSetState(0, acquires)) {  // ← 直接抢！
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // ... 重入逻辑
}
```

| | 公平锁 | 非公平锁 |
|---|---|---|
| **获取顺序** | 按请求顺序，FIFO | 谁抢到是谁的 |
| **性能** | 较低（每次都要查队列） | **更高**（减少线程切换） |
| **饥饿** | 无 | 可能存在 |
| **默认** | — | **ReentrantLock 默认非公平** |

> **为什么默认非公平？** 新线程 CAS 抢锁如果正好成功 → 省去一次线程挂起/恢复的上下文切换开销。

### 2.3 Condition 精确唤醒

```java
Lock lock = new ReentrantLock();
Condition notFull = lock.newCondition();   // 队列不满条件
Condition notEmpty = lock.newCondition();  // 队列不空条件

// 生产者
lock.lock();
try {
    while (queue.isFull())
        notFull.await();          // 等"不满"信号
    queue.add(item);
    notEmpty.signal();            // 精确唤醒消费者（而非唤醒所有）
} finally {
    lock.unlock();
}

// 消费者
lock.lock();
try {
    while (queue.isEmpty())
        notEmpty.await();         // 等"不空"信号
    queue.take();
    notFull.signal();             // 精确唤醒生产者
} finally {
    lock.unlock();
}
```

**vs synchronized 的 wait/notify**：
- synchronized 只能用对象本身的一个条件 → `obj.wait()` / `obj.notifyAll()` 唤醒所有
- ReentrantLock 可创建**多个 Condition** → 精确唤醒具体的等待线程组

---

## 三、CAS（Compare And Swap）无锁算法核心

### 3.1 CAS 是什么

```java
// CAS 是一条 CPU 原子指令（cmpxchg）
// 语义：比较内存值 V 与预期值 A，相同则更新为 B，不同则不做任何操作

boolean compareAndSet(int expectedValue, int newValue) {
    // 伪代码：读取当前值 → 比较 → 相等则写入 → 返回是否成功
    if (currentValue == expectedValue) {
        currentValue = newValue;
        return true;
    }
    return false;
}
// 整个"比较+交换"过程由 CPU 指令保证原子性，不会被中断
```

### 3.2 CAS 在 Java 中的使用

```java
AtomicInteger ai = new AtomicInteger(0);

// CAS 自旋更新
int oldValue, newValue;
do {
    oldValue = ai.get();           // 读取当前值
    newValue = oldValue + 1;       // 计算新值
} while (!ai.compareAndSet(oldValue, newValue));  // CAS 更新，失败则重试
```

`AtomicInteger.incrementAndGet()` 内部就是上面的 CAS 自旋循环。

### 3.3 CAS 的三大问题

| 问题 | 说明 | 解决方案 |
|---|---|---|
| **ABA 问题** | 值从 A → B → A，CAS 检测不到中间变化 | `AtomicStampedReference`（版本号） / `AtomicMarkableReference` |
| **循环开销** | CAS 失败时自旋重试，消耗 CPU | 竞争激烈时用 `LongAdder`（分段累加）或 `synchronized` |
| **单变量限制** | CAS 只能保证一个共享变量的原子操作 | 封装成对象用 `AtomicReference` |

### 3.4 ABA 问题详解

```
时间线：
线程 1: 读 value = A，准备 CAS 改为 C
线程 2: CAS 把 A → B
线程 2: CAS 把 B → A    ← 值变回了 A，但中间已被"碰过"
线程 1: CAS A → C 成功  ← 误以为值没变过！

AtomicStampedReference 解决：
  [value=A, stamp=0] → 线程2改为 [B, 1] → 线程2改为 [A, 2]
  线程1 CAS 时：期望 stamp=0，实际 stamp=2 → 失败 → 重试
```

```java
// ABA 问题解决示例
AtomicStampedReference<Integer> ref = 
    new AtomicStampedReference<>(100, 0);  // 初始值100，版本0

int stamp = ref.getStamp();
int value = ref.getReference();

// CAS 同时检查值和版本号
ref.compareAndSet(value, newValue, stamp, stamp + 1);
```

---

## 四、原子类家族

### 4.1 原子类分类

| 类别 | 类 | 特点 |
|---|---|---|
| **基本类型** | `AtomicInteger`、`AtomicLong`、`AtomicBoolean` | CAS 操作单变量 |
| **数组** | `AtomicIntegerArray`、`AtomicLongArray`、`AtomicReferenceArray` | CAS 操作数组元素 |
| **引用** | `AtomicReference` | CAS 操作对象引用 |
| **字段更新** | `AtomicIntegerFieldUpdater` | CAS 更新 volatile 字段 |
| **ABA 解决** | `AtomicStampedReference`（版本号） / `AtomicMarkableReference` | 带标记的 CAS |
| **高性能** | `LongAdder`、`DoubleAdder` | 分段累加，高并发下远超 AtomicLong |

### 4.2 LongAdder 为什么比 AtomicLong 快？

```
AtomicLong:
  所有线程竞争同一个变量 → CAS 失败率随并发增长 → 自旋浪费 CPU

LongAdder:
  ┌────────────┐
  │  base 值   │ ← 低竞争时直接 CAS 更新 base
  └────────────┘
  ┌──┐ ┌──┐ ┌──┐
  │C0│ │C1│ │C2│ ...  ← 高竞争时每个线程去独立的 Cell 累加
  └──┘ └──┘ └──┘
  sum() = base + Cell[0] + Cell[1] + ...

  写入时只 CAS 自己的那个 Cell → 冲突大幅降低
  读取时 sum() 汇总所有 Cell + base（不是实时精确值）
```

---

## 五、ReentrantLock 的 Condition 实现原理

```java
// AQS 内部维护两个队列：
// 1. Sync Queue（同步队列）：竞争锁的线程（CLH 队列）
// 2. Condition Queue（条件队列）：调用 await() 的线程

await()  →  当前线程节点从 Sync Queue 移到 Condition Queue → 释放锁 → 阻塞
signal() →  从 Condition Queue 移出第一个节点 → 放入 Sync Queue 尾部 → 等待获取锁
```

**注意**：`signal()` 只是把线程从条件队列移到同步队列，并不立即唤醒 → 只有当前线程释放锁后，被移过去的线程才有机会获取。

---

## 六、终极背诵总结

1. **AQS 核心**：state（同步状态）+ CLH 变体 FIFO 队列 + CAS 操作，模板方法模式
2. **AQS 实现**：ReentrantLock / Semaphore / CountDownLatch / ReentrantReadWriteLock 都基于 AQS
3. **ReentrantLock 可重入**：state=1 首次获取，state++ 表示重入次数，释放时 state-- 到 0 才真正解锁
4. **公平 vs 非公平**：公平锁检查队列（FIFO），非公平锁直接 CAS 抢（性能更高，默认）
5. **CAS 原理**：CPU 原子指令 cmpxchg，比较内存值与预期值，相同则交换
6. **ABA 问题**：AtomicStampedReference 加版本号解决
7. **LongAdder 优化**：base + Cell[] 分段累加，减少 CAS 碰撞，高并发远超 AtomicLong
8. **Condition 优势**：多个条件队列，精确唤醒，比 wait/notifyAll 更精细
