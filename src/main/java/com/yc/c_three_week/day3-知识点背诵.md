# Day3 synchronized 底层原理与锁升级 核心知识点背诵笔记 ⭐⭐⭐

---

## 一、synchronized 本质 —— 对象监视器锁（Monitor）

### 1.1 核心定义

`synchronized` 是 Java 内置的**互斥同步锁**，基于 JVM 的 **Monitor（监视器锁）** 机制实现。每个 Java 对象在 JVM 内部都与一个 Monitor 关联。

### 1.2 三种使用方式

```java
// ① 修饰普通方法 → 锁是当前实例对象 this
public synchronized void method() { }

// ② 修饰静态方法 → 锁是当前类的 Class 对象
public static synchronized void staticMethod() { }

// ③ 同步代码块 → 锁是括号内指定的对象
synchronized (obj) { }
```

**记忆**：普通方法锁 this，静态方法锁 Class，代码块锁任意对象。

### 1.3 字节码层面

```java
// synchronized 代码块 → 编译后生成 monitorenter / monitorexit 指令
synchronized (obj) {
    // ...
}
// 字节码：
// monitorenter    ← 进入同步块，尝试获取 obj 的 Monitor
//   ... 业务代码 ...
// monitorexit     ← 正常退出，释放 Monitor
// monitorexit     ← 异常退出路径（编译器自动生成的隐式 finally）

// synchronized 方法 → 方法表里 ACC_SYNCHRONIZED 标志位
// JVM 检查到该标志 → 自动在调用前 monitorenter，返回/异常时 monitorexit
```

---

## 二、Java 对象头与 Mark Word（理解锁升级的钥匙）

### 2.1 对象内存布局

```
Java 对象内存布局（HotSpot）：
┌──────────────────┐
│   Mark Word      │ ← 32/64位，存 hashcode、锁状态、GC 分代年龄
├──────────────────┤
│   Klass Pointer  │ ← 指向方法区 Class 元数据的指针
├──────────────────┤
│   实例数据        │ ← 成员变量
├──────────────────┤
│   对齐填充        │ ← 保证 8 字节对齐
└──────────────────┘
```

### 2.2 Mark Word 在 5 种锁状态下的结构变化

锁升级本质就是**Mark Word 内容的演变**：

```
无锁态：  [ unused:25 | hashcode:31 | unused:1 | age:4 | biased_lock:0 | 01 ]
偏向锁：  [ thread_id:54 | epoch:2 | unused:1 | age:4 | biased_lock:1 | 01 ]
轻量级锁：[ ptr_to_lock_record:62                                | 00 ]
重量级锁：[ ptr_to_monitor:62                                    | 10 ]
GC 标记： [ 空                                                 | 11 ]
```

> 关键：Mark Word 的最低 3 位（biased_lock + lock 标志）决定锁状态。

---

## 三、锁升级完整路径（JDK 6+ 优化核心）⭐⭐⭐

### 3.1 升级路径全览

```
无锁
  │ 有线程第一次获取锁
  ▼
偏向锁（Biased Lock）
  │ 有其他线程竞争，偏向锁撤销
  ▼
轻量级锁（Lightweight Lock）
  │ CAS 自旋，自旋失败 / 竞争激烈
  ▼
重量级锁（Heavyweight Lock）
  │ OS mutex 阻塞
  ▼
线程阻塞，等待操作系统调度
```

> **锁只能升级不能降级**（偏向锁可被批量撤销，但这是 JVM 优化行为）

### 3.2 偏向锁（Biased Locking）

**原理**：
- 第一次获取锁时，用 CAS 将**当前线程 ID** 写入 Mark Word
- 之后**同一线程**重入时，只需检查 Thread ID 是否匹配 → 无需 CAS，零开销 ⭐
- 没有实际竞争时，一个线程反复获取同一把锁几乎无性能损耗

**偏向锁撤销（何时升级到轻量级锁）**：
- 有其他线程试图获取这把锁 → 到达**安全点** → 检查偏向线程是否存活 → 撤销偏向锁
- 撤销成本较高 → 所以在竞争多、线程多的场景，偏向锁反而拉低性能

**JDK 15+ 默认关闭偏向锁**：现代应用并发度高，偏向锁撤销开销大于收益。

### 3.3 轻量级锁（Lightweight Lock）

**原理**：

```
线程 A 获取锁：
  1. 在当前栈帧中创建 Lock Record
  2. 将锁对象的 Mark Word 拷贝到 Lock Record（称为 Displaced Mark Word）
  3. CAS 将锁对象 Mark Word 替换为指向 Lock Record 的指针
  4. CAS 成功 → 获取轻量级锁成功
  5. CAS 失败 → 进入自旋

线程 B 竞争（轻量级锁膨胀为重量级锁）：
  - 自旋若干次 → 仍失败 → 锁膨胀
  - JVM 创建 Monitor 对象
  - 锁对象 Mark Word 指向 Monitor
  - 线程 B 进入 Monitor 的 EntryList 阻塞
```

**自旋的考量**：
- **自旋次数**：JDK 6 之前固定 10 次，之后**自适应自旋**（根据上次自旋结果动态决定）
- 自旋不释放 CPU → 适合**临界区短**的场景（避免挂起/恢复的开销）
- 如果临界区长 → 自旋是浪费 CPU，应尽快膨胀为重量级锁

### 3.4 重量级锁（Heavyweight Lock）

**原理**：
- JVM 向 OS 申请 **mutex（互斥量）** 实现
- 未获取锁的线程**阻塞挂起**，进入 Monitor 的 **EntryList** 队列
- 线程被 OS 调度挂起 → 上下文切换成本大（用户态 ↔ 内核态）
- 但 CPU 不空转，适合**锁持有时间长**的场景

**Monitor 内部结构**：

```
┌──────── Monitor ────────┐
│ Owner: 当前持有锁的线程   │
│                         │
│ EntryList: 等锁的线程队列 │ → 这些线程处于 BLOCKED
│                         │
│ WaitSet: wait() 的线程   │ → 这些线程处于 WAITING
└─────────────────────────┘
```

---

## 四、锁升级的完整线程交互

```
线程 A 第一次获取锁：
  偏向锁 → Mark Word 写入 Thread A 的 ID

线程 A 再次获取：
  检查 Mark Word 中 Thread ID == A → 直接进入，零开销

线程 B 尝试获取：
  发现偏向锁偏向 Thread A
  → 到达全局安全点（STW 暂停）
  → 检查 A 是否还活着
      ├─ A 已结束 → 撤销偏向，CAS 改为偏向 B
      └─ A 还活着 → 撤销偏向，升级为轻量级锁
          → A 的栈帧创建 Lock Record
          → B CAS 自旋竞争

B 自旋失败 / 竞争加剧：
  → 轻量级锁膨胀为重量级锁
  → JVM 创建 Monitor 对象
  → B 进入 EntryList 阻塞（BLOCKED）
  → A 释放锁时唤醒 EntryList 中的线程
```

---

## 五、synchronized vs ReentrantLock 五大区别

| 对比维度 | synchronized | ReentrantLock |
|---|---|---|
| **本质** | JVM 关键字，编译器识别 | JDK API（`java.util.concurrent.locks`） |
| **锁释放** | **自动释放**（代码块结束/异常） | **手动释放**（必须 finally unlock） |
| **可中断** | 不可中断，等不到锁就一直等 | `lockInterruptibly()` 可中断 |
| **公平锁** | 非公平（不保证先来先得） | 可选公平/非公平（构造器传入） |
| **条件唤醒** | wait/notify，只能一个条件 | **多个 Condition**，精确唤醒 |
| **尝试获取** | 不支持（等不到就堵死） | **tryLock()** 尝试拿锁，拿不到先干别的 |
| **性能** | JDK 6+ 优化后基本持平 | 略高一点点（API 调用开销） |

---

## 六、wait / notify 与 Monitor

### 6.1 执行流程

```java
synchronized (obj) {
    obj.wait();    // ① 当前线程进入 WaitSet
                   // ② 释放 Monitor 所有权
                   // ③ 阻塞等待 notify
                   // ④ 被通知后 → 重新竞争 Monitor → 拿到后继续执行
}

synchronized (obj) {
    obj.notify();  // ① 从 WaitSet 随机移出一个线程
                   // ② 该线程竞争 Monitor（此时 Monitor 仍被当前线程持有）
                   // ③ 当前线程出 synchronized 块 → 释放 Monitor
                   // ④ 被 notify 的线程获得 Monitor 继续执行
}
```

### 6.2 为什么 wait 要包在 while 里？

```java
// ❌ 错误写法
synchronized (obj) {
    if (condition) obj.wait();  // 被唤醒后直接执行，不重新检查
}

// ✅ 正确写法
synchronized (obj) {
    while (condition) obj.wait();  // 醒来后循环检查条件
}
```

**原因**：虚假唤醒（spurious wakeup）。操作系统可能「无通知就唤醒」线程，如果不循环检查，线程可能在条件不满足时就往下执行了。

---

## 七、终极背诵总结

1. **本质**：synchronized = 对象 Monitor 锁，进入 monitorenter，退出 monitorexit
2. **三种用法**：普通方法锁 this、静态方法锁 Class、代码块锁指定对象
3. **锁升级**：无锁 → 偏向锁（CAS 写线程ID）→ 轻量级锁（CAS 自旋）→ 重量级锁（OS mutex）
4. **偏向锁**：单线程反复进入场景，零开销重入
5. **轻量级锁**：多线程交替执行（非同时竞争），自旋避免阻塞
6. **重量级锁**：竞争激烈，线程阻塞让出 CPU，OS 调度
7. **Mark Word**：对象头前半部分，锁状态变化就是 Mark Word 最低位的变化
8. **vs ReentrantLock**：自动释放 vs 手动释放、不可中断 vs 可中断、单一条件 vs 多 Condition
