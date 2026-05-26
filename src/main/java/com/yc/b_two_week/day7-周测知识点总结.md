# Day7 周测知识点总结 —— Java 集合框架与常用源码

> 覆盖 Day1~Day6 全部核心知识点，对应"学习任务清单" Day 7 整周复盘要求。

---

## 一、10 道自测题（口述标准答案）

### Q1: ArrayList 扩容机制（从 add 到扩容完整讲一遍）

**答：**

1. 调用 `add(E e)` 时，先调用 `ensureCapacityInternal(size + 1)` 判断是否需要扩容
2. 如果是**首次添加**（JDK 7+懒加载），elementData 指向空数组，取 `max(10, minCapacity)` 分配初始容量
3. 非首次添加，若 `size + 1 > elementData.length`，调用 `grow(minCapacity)`
4. `grow()` 计算新容量：`newCapacity = oldCapacity + (oldCapacity >> 1)` → **1.5 倍**
5. 若 1.5 倍还不够，直接用 `minCapacity`
6. 若超过 `MAX_ARRAY_SIZE`（Integer.MAX_VALUE - 8），调用 `hugeCapacity()` 处理
7. 最后 `Arrays.copyOf(elementData, newCapacity)` → 底层 `System.arraycopy()` native 方法拷贝元素到新数组

**口诀**：`add → ensureCapacity → grow → 1.5倍 → Arrays.copyOf → arraycopy 搬元素`

---

### Q2: ArrayList 和 LinkedList 各适合什么场景？为什么？

**答：**

| 场景 | 选谁 | 原因 |
|---|---|---|
| **查多写少**、随机访问频繁 | **ArrayList** | 底层数组，`get(index)` O(1)，内存连续 CPU 缓存友好 |
| **频繁头尾增删**、少随机访问 | **LinkedList** | 头尾 O(1)，改指针即可；同时 `Deque` 接口可做栈/队列 |
| **频繁中间增删** | 差距不大 | 两者都需先定位（O(n)），之后 ArrayList 搬元素、LinkedList 改指针 |
| **遍历** | ArrayList | ArrayList 用普通 for；LinkedList 必须用迭代器/增强 for |

> **实际开发默认选 ArrayList**，大部分业务读多写少；LinkedList 节点额外存两个指针，内存开销大。

---

### Q3: HashMap 的 put 流程（从 hash 计算到写入到扩容）

**答（8 步全链路）：**

```
① hash(key) → (h = key.hashCode()) ^ (h >>> 16)     → 高低位异或减少碰撞
② (n-1) & hash 定位桶位                                → 位运算等价于取模
③ 桶空 → 直接 newNode 放入
④ 桶首节点 key 相等（hash同 && (== 或 equals)）   → 覆盖旧值
⑤ 桶首是 TreeNode → 走红黑树插入逻辑
⑥ 遍历链表 → 尾插法 → 逐个比较 key 是否相等
⑦ 链表长度 ≥ 8 → treeifyBin() → 数组 < 64 则扩容，≥ 64 则树化
⑧ ++size > threshold → resize() 扩容（容量翻倍）
```

**口诀**：`hash → 定桶 → 空直放 → 等覆盖 → 树走树链尾插 → 计超八试树化 → 超阈值扩容`

---

### Q4: HashMap 容量为什么是 2 的幂？不是会怎样？

**答：**

**原因（3 点）**：
1. **位运算取模**：`(n-1) & hash` 等价于 `hash % n`，位运算比取模快一个数量级
2. **均匀分布**：n 为 2 的幂时，n-1 低位全是 1，与 hash 做 & 运算，索引分布均匀
3. **扩容优化**：`hash & oldCap` 判断节点去留（0 原位，非 0 去原位置+旧容量），不需要重新计算 hash

**如果不是 2 的幂**：
- n-1 的二进制低位有空缺，部分桶位永远分配不到元素 → 空间浪费 + 严重哈希冲突
- `new HashMap(10)` 实际容量被 `tableSizeFor()` 向上修正为 16

---

### Q5: JDK 7 的 HashMap 为什么会死循环？

**答：**

**根因**：**头插法 + 多线程并发 resize**

**推演**：
1. JDK 7 resize 时采用头插法迁移节点，链表顺序被**反转**
2. 线程 A 正在迁移（e=A, next=B）时挂起；线程 B 完成整表迁移（变成 B→A）
3. 线程 A 恢复后，仍按之前保存的 next 指针操作，但链表已被线程 B 反转
4. 最终形成 **A↔B 环形链表**（A.next=B, B.next=A）
5. 后续 `get()` 查询落到该桶 → 死循环遍历 → CPU 100%

**JDK 8 解决**：
- **尾插法**（不反转顺序）+ **高低位分流**（一次遍历分两条链，不交叉）
- 多线程最多导致数据丢失，不会形成环形链表

---

### Q6: 链表转红黑树的两个条件是什么？

**答：**

```java
// 条件1: 链表节点数 ≥ 8（TREEIFY_THRESHOLD）
if (binCount >= TREEIFY_THRESHOLD - 1)   // 在 putVal 中
    treeifyBin(tab, hash);

// 条件2: 数组长度 ≥ 64（MIN_TREEIFY_CAPACITY）
if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)  // 在 treeifyBin 中再判断
    resize();   // 数组太小 → 扩容而非树化
```

**两个条件缺一不可**：
- 链表 ≥ 8 **且** 数组 ≥ 64 → 树化
- 链表 ≥ 8 但数组 < 64 → **只扩容不树化**（扩容后节点重新散列，链表自然缩短）

**为什么是 8？** 在 loadFactor=0.75 下，桶内节点数服从 λ=0.5 的泊松分布，达到 8 的概率 < 0.00000006（千万分之一），能到 8 说明哈希异常。

**退化**：树节点 ≤ 6 时退化为链表。8 和 6 中间留 7 缓冲，避免频繁转换。

---

### Q7: HashMap 线程不安全体现在哪些场景？

**答：**

| 场景 | 问题 |
|---|---|
| **JDK 7 并发 resize** | 头插法形成环形链表 → `get()` 死循环，CPU 100% |
| **JDK 8 并发 put** | 两个线程同时 put 不同 key 到同一桶 → 尾插法可能导致数据**丢失**（一个覆盖另一个，但不会死循环） |
| **并发 put + get** | get 可能读到不一致的数据（正在 resize 时部分数据在旧表部分在新表） |
| **size 不准确** | size 字段不是 volatile，多线程下 `size()` 返回值不精确 |

> **结论**：多线程必须用 `ConcurrentHashMap`，禁止用 HashMap + 手动同步。

---

### Q8: ConcurrentHashMap 1.7 和 1.8 的并发实现有什么区别？

**答：**

| 对比维度 | CHM 1.7 | CHM 1.8 |
|---|---|---|
| **并发单位** | **Segment**（默认 16 段），每段独立的 ReentrantLock | **桶首节点**，synchronized 锁单个桶 |
| **数据结构** | Segment 数组 + HashEntry 数组 + 链表 | Node 数组 + 链表 + 红黑树（与 HashMap 统一） |
| **写操作** | lock() 锁一个 Segment | 桶空 → **CAS** 无锁放入；桶非空 → **synchronized** 锁桶首节点 |
| **读操作** | **无锁**（volatile 可见性） | **无锁**（volatile + Unsafe.getObjectVolatile） |
| **扩容** | Segment 内部独立扩容 | **多线程协同迁移**，CAS 竞争 transferIndex，分步推进 |
| **并发度** | 固定 ≤ 16 | **桶数级别**，理论并发度 = 桶数 |
| **内存开销** | 大（每个 Segment 独立维护 table + 锁） | 小（结构统一，无 Segment 层） |

---

### Q9: HashSet 怎么保证元素不重复？

**答：**

**HashSet 底层就是一个 HashMap**。

```java
private transient HashMap<E,Object> map;
private static final Object PRESENT = new Object();  // 哑元常量

public boolean add(E e) {
    return map.put(e, PRESENT) == null;  // key不存在→返回null→add成功
}
```

- 元素作为 HashMap 的 **key** 存储，value 是固定的 `PRESENT` 哑元对象
- 依靠 **HashMap key 的唯一性**：hashCode 定桶 + equals 判等
- 两个元素 hashCode 相等且 equals 为 true → 后者覆盖前者 → Set 元素不重复

同样：
- `LinkedHashSet` → 底层 `LinkedHashMap`
- `TreeSet` → 底层 `TreeMap`（用 compareTo/compare 判重，返回 0 视为重复）

---

### Q10: fail-fast 机制是怎么实现的？ConcurrentHashMap 为什么不会有这问题？

**答：**

**fail-fast 实现原理**（ArrayList / HashMap）：

```java
protected transient int modCount = 0;  // 结构修改计数器

// 迭代器创建时快照
int expectedModCount = modCount;

// 每次 next() 校验
final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

- 集合中有一个 `modCount` 计数器，每次结构修改（add/remove）自增
- 迭代器初始化时拷贝一份 `expectedModCount`
- 每次 **next() 都检查两个值是否一致**，不一致立即抛异常
- **foreach 底层就是迭代器**，所以 foreach 中直接 list.remove() 也会触发

**ConcurrentHashMap 为什么不用 fail-fast？**

- CHM 设计目标就是**支持并发修改**，使用 fail-fast 没有意义
- 它的迭代器是**弱一致性（fail-safe）**：遍历的是原数组的"快照"，不调用 checkForComodification
- 允许在遍历过程中插入/删除数据，但**不保证读到最新值**（弱一致性）
- CHM 通过 volatile + CAS 保证基本可见性，但迭代器不做并发修改检测

---

## 二、集合框架知识脑图

```
Java 集合框架（java.util / java.util.concurrent）

┌─────────────────────────────────────────────────────────────┐
│                        Collection 接口                       │
│                     （存储单个元素）                           │
├─────────────────┬──────────────────┬────────────────────────┤
│     List        │       Set        │        Queue           │
│  有序 可重复     │  不可重复          │    FIFO/优先级          │
├─────────────────┼──────────────────┼────────────────────────┤
│ ■ ArrayList     │ ■ HashSet        │ ■ LinkedList           │
│   数组 查O(1)    │   HashMap实现     │   (也实现Deque)         │
│   扩容1.5倍      │   无序去重         │ ■ PriorityQueue        │
│   线程不安全      │                  │   二叉堆                │
│                 │ ■ LinkedHashSet  │ ■ ArrayDeque           │
│ ■ LinkedList    │   有序去重         │   循环数组               │
│   双向链表       │                  │                        │
│   头尾O(1)      │ ■ TreeSet        │                        │
│   实现Deque     │   红黑树排序去重    │                        │
│                 │   TreeMap实现     │                        │
│ ■ Vector ▸已淘汰│                  │                        │
│   synchronized  │                  │                        │
│   扩容2倍        │                  │                        │
└─────────────────┴──────────────────┴────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        Map 接口                              │
│                    （存储键值对）                              │
├──────────────────────┬──────────────────────────────────────┤
│    非线程安全          │         线程安全                      │
├──────────────────────┼──────────────────────────────────────┤
│ ■ HashMap            │ ■ Hashtable ▸已淘汰                   │
│   数组+链表+红黑树     │   synchronized 全表锁                 │
│   容量16 负载0.75     │   不允许 null                        │
│   扩容2倍 树化8退6    │                                      │
│                      │ ■ ConcurrentHashMap                  │
│ ■ LinkedHashMap      │   JDK7: 分段锁Segment(16段)           │
│   继承HashMap         │   JDK8: CAS+synchronized桶首节点      │
│   双向链表保序         │   读无锁 多线程协同扩容                  │
│   可实现LRU           │   数组+链表+红黑树                     │
│                      │                                      │
│ ■ TreeMap            │ ■ Collections.synchronizedMap()      │
│   红黑树              │   包装后全表synchronized               │
│   Key排序             │   本质同Hashtable                     │
└──────────────────────┴──────────────────────────────────────┘
```

---

## 三、HashMap 高频题笔记

### 3.1 核心参数速记

| 参数 | 值 | 含义 |
|---|---|---|
| 默认容量 | 16（1 << 4） | 必须 2 的幂 |
| 最大容量 | 1 << 30 | 约 10 亿 |
| 负载因子 | 0.75 | 时间空间折中点 |
| 树化阈值 | 8 | 链表→红黑树（概率 < 千万分之一） |
| 退化阈值 | 6 | 红黑树→链表 |
| 最小树化容量 | 64 | 数组 < 64 不树化，优先扩容 |

### 3.2 put 流程（面试速答版）

```
1. hash = (h = key.hashCode()) ^ (h >>> 16)
2. index = (n - 1) & hash
3. table[index] == null → newNode → done
4. 首节点 hash 同且 key 等 → 覆盖 value
5. 首节点是 TreeNode → putTreeVal
6. 遍历链表：
   - 找到同 key → 覆盖
   - 没找到 → 尾插新节点
   - binCount ≥ 7 → treeifyBin → 数组≥64 树化 / 否则扩容
7. ++size > threshold → resize
```

### 3.3 JDK 7 vs JDK 8 五维对比

| 维度 | JDK 1.7 | JDK 1.8 |
|---|---|---|
| 结构 | 数组+链表 | 数组+链表+红黑树 |
| 插入 | **头插法** | **尾插法** |
| hash | 4次位运算+5次异或 | `h ^ h>>>16`（1次异或） |
| resize | 头插倒序重建 | hash&oldCap 高低位分流 |
| 死循环 | **有** | **无** |

### 3.4 resize 扩容速记

```
触发: size > capacity × 0.75
动作: newCap = oldCap × 2, newThr = oldThr × 2
迁移: 遍历旧表每个桶
  - 单节点 → newTab[hash & (newCap-1)] = e
  - 链表 → 按 hash & oldCap 分高低两条链
    - 0 → 原位
    - 非0 → 原位 + oldCap
  - 树 → split 拆分（可能退化回链表）
```

### 3.5 树化 / 退化条件

```
链表长度 ≥ 8  ──────────────────────────────────────────────→  红黑树
                    ↑ 前提: table.length ≥ 64              ↑
                    │ 否则扩容                                │
红黑树节点 ≤ 6  ←──────────────────────────────────────────  链表
                    (7 为缓冲，避免频繁转换)
```

### 3.6 HashMap 线程不安全三场景

1. **JDK 7 死循环**：并发 resize + 头插法 → 环形链表 → CPU 100%
2. **JDK 8 数据丢失**：并发 put 到同一桶，尾插法互相覆盖
3. **size 不准确**：size 非 volatile，多线程下不一致

---

## 四、集合选型经验总结

### 4.1 选型决策速查

```
需要存储 ─┬─ 单个元素 ─┬─ 有序/可重复 ───→ ArrayList（默认首选）
          │            │                      └→ LinkedList（频繁头尾操作时）
          │            │
          │            ├─ 去重 ─────┬─ 不关心顺序 → HashSet
          │            │            ├─ 保留插入序 → LinkedHashSet
          │            │            └─ 需要排序 → TreeSet
          │            │
          │            └─ 队列/栈 ─────→ LinkedList / ArrayDeque
          │
          └─ 键值对 ──┬─ 单线程 ────┬─ 不关心顺序 → HashMap（首选）
                       │             ├─ 保留插入序 → LinkedHashMap
                       │             └─ 需要排序 → TreeMap
                       │
                       └─ 多线程 ────→ ConcurrentHashMap（必选）
                                      禁止用 Hashtable
```

### 4.2 各场景首选方案

| 业务场景 | 首选集合 | 理由 |
|---|---|---|
| 日常列表存储 | **ArrayList** | 查多写少，内存紧凑，CPU 友好 |
| 消息队列/任务调度 | **LinkedList** 或 **ArrayDeque** | 头尾操作 O(1) |
| URL 去重/黑名单 | **HashSet** | O(1) 去重，不关心顺序 |
| 需要排序的排行榜 | **TreeSet** / **TreeMap** | 红黑树自动排序 |
| LRU 缓存 | **LinkedHashMap** | accessOrder=true 自动淘汰 |
| 高并发缓存/注册表 | **ConcurrentHashMap** | 读无锁，写锁桶，并发度高 |
| 配置项/字典 | **HashMap** | 单线程下性能最好 |

### 4.3 禁止/避免事项

| ❌ 禁止 | ✅ 替代 |
|---|---|
| 多线程用 HashMap | `ConcurrentHashMap` |
| 用 Hashtable | `ConcurrentHashMap` |
| 用 Vector | `ArrayList` + 外部同步或 `CopyOnWriteArrayList` |
| foreach 中直接 list.remove() | 迭代器的 `iterator.remove()` |
| 循环中用 String + 拼接 | `StringBuilder` |
| `new ArrayList<>()` 不预设容量 | `new ArrayList<>(预估量)` 减少扩容 |

### 4.4 时间复杂度速查

| 操作 | ArrayList | LinkedList | HashSet/HashMap |
|---|---|---|---|
| 随机访问 get(index) | **O(1)** | O(n) | — |
| 尾部添加 | 均摊 O(1) | O(1) | O(1) |
| 中间插入 | O(n) | O(n)（定位）+ O(1) | — |
| 查找元素 | O(n) | O(n) | **O(1)** |
| 删除 | O(n) | O(n)（定位）+ O(1) | **O(1)** |

---

## 五、本周重点代码清单（手写必备）

| 序号 | 代码 | 对应文件 | 关键点 |
|---|---|---|---|
| 1 | 验证 Comparator 优先于 Comparable | W2D1_Actual + Student | `Collections.sort` 传入外部比较器覆盖内部 |
| 2 | ArrayList 5 种遍历方式 | W2D1_Actual | for / 增强for / Iterator / forEach / Stream |
| 3 | 集合选型总结 | W2D1_Actual 注释 | List / Set / Map 各实现类选型矩阵 |
| 4 | 简化版动态数组 | W1D2_Actual(待补充) | add / get / remove / 扩容 1.5 倍 |
| 5 | ArrayList vs LinkedList 性能对比 | W2D3_Actual(待补充) | 头/中/尾 插入 10 万条耗时对比 |
| 6 | HashMap put 流程图 | day4-知识点背诵.md | 8 步链路 + 树化/扩容分支 |
| 7 | 自定义对象做 HashMap key | 待手写 | hashCode 对存取影响的验证 |
| 8 | LRU 缓存 | 待手写 | LinkedHashMap accessOrder 实现 |
| 9 | Hashtable vs CHM 并发 put 性能 | 待手写 | 多线程并发对比 |
| 10 | CHM 迭代器弱一致性验证 | 待手写 | 遍历中插入新元素 |

---

## 六、速记口诀

1. **集合体系**：Collection 存单个，Map 存键值对，List 有序可重复，Set 去重各不同
2. **ArrayList 扩容**：首次懒加载，add 判扩容，1.5 倍增长，Arrays.copyOf 搬
3. **LinkedList**：双向链表 Deque 通，头尾 O(1) 改指针，随机访问 O(n) 找位置
4. **HashMap put**：hash 异或定桶位，空直放等覆盖，树走树链尾插法，超八试树超阈扩
5. **HashMap 2 的幂**：位运算取模快，分布均，扩容高位低位自然分
6. **JDK 7 死循环**：头插倒序多线程，环形链表 CPU 崩
7. **树化条件**：链表八数组四六四，双条缺一不成树
8. **退化条件**：树节六退回链表，缓冲七免频繁跳
9. **CHM 演进**：Hashtable 全表锁，CHM 7 分段锁，CHM 8 CAS+桶首锁
10. **HashSet 本质**：HashMap 当底层，元素当 Key 哑元填，hashCode 定位 equals 判
11. **fail-fast**：modCount 计数器，迭代器对账不一致就抛异常
12. **选型口诀**：查多用 ArrayList，头尾多用 LinkedList，去重用 Set，映射用 Map，并发必用 CHM
