# ConcurrentHashMap 与并发集合面试核心笔记
---

## 一、ConcurrentHashMap 版本对比（面试必背）
### 1. JDK 7 实现
- **核心机制**：分段锁（Segment）
    - 默认分为 16 个 Segment 段，每个段独立加锁
    - 并发度 = Segment 数量，多线程可同时操作不同 Segment
- **缺点**：
    - 锁粒度较粗，高并发下仍有竞争
    - 实现复杂，维护成本高

### 2. JDK 8 实现（主流考点）
- **核心机制**：`CAS + synchronized`（锁桶首节点）
    - 底层结构与 HashMap 统一：`数组 + 链表 + 红黑树`
    - 锁粒度细化到**单个桶**，仅对链表/红黑树的头节点加锁
    - 无锁操作：使用 `CAS` 实现桶的初始化与空节点插入
- **优势**：
    - 并发度大幅提升，冲突时仅阻塞单个桶的操作
    - 实现更简洁，性能优于分段锁

---

## 二、并发 Map 对比表
| 实现类 | 锁机制 | 并发性能 | 底层结构 | 适用场景 |
| :--- | :--- | :--- | :--- | :--- |
| Hashtable | 全表锁（`synchronized` 修饰所有方法） | 极差 | 数组 + 链表 | 老旧代码兼容，不推荐使用 |
| Collections.synchronizedMap | 方法级同步锁（对象锁） | 一般 | 包装 HashMap | 低并发场景，读写竞争少 |
| ConcurrentHashMap 7 | 分段锁（Segment） | 较好 | Segment 数组 + 链表 | 旧版本 JDK 兼容 |
| ConcurrentHashMap 8+ | CAS + 桶节点锁 | 极高 | 数组 + 链表 + 红黑树 | 高并发场景（推荐） |

---

## 三、HashSet 底层原理
- **底层实现**：基于 `HashMap`
    - 存入 `HashSet` 的元素，会作为 `HashMap` 的 `key`
    - `value` 是一个固定的 `Object` 对象（常量 `PRESENT`）
- **核心特性**：
    - 不允许重复元素，依赖 `HashMap` 的 `key` 唯一性
    - 线程不安全，并发场景推荐使用 `CopyOnWriteArraySet`

---

## 四、迭代器机制：fail-fast vs fail-safe
### 1. fail-fast（快速失败）
- **定义**：遍历集合时，若集合被并发修改（如增删元素），立即抛出 `ConcurrentModificationException`
- **实现原理**：通过 `modCount`（修改次数）与 `expectedModCount` 比较
- **典型场景**：`HashMap`、`ArrayList` 等非并发集合的迭代器
- **注意**：fail-fast 是一种“尽最大努力”的机制，不能保证一定抛出异常

### 2. fail-safe（安全失败）
- **定义**：遍历集合时，允许并发修改，不抛出异常
- **实现原理**：
    - 遍历的是集合的**副本**（如 `CopyOnWriteArrayList`），或基于弱一致性视图
    - `ConcurrentHashMap` 迭代器是弱一致性，遍历过程中允许修改，不保证最新数据
- **典型场景**：`ConcurrentHashMap`、`CopyOnWriteArrayList` 等并发集合的迭代器

---

## 五、高频面试题速记
1. **ConcurrentHashMap 为什么比 Hashtable 性能好？**
    - Hashtable 是全表锁，所有线程竞争同一把锁；
    - ConcurrentHashMap 锁粒度更细（分段锁/桶节点锁），多线程可并发操作不同桶，冲突概率低。

2. **ConcurrentHashMap JDK 7 和 8 的区别？**
    - 7：分段锁 Segment，锁粒度粗；
    - 8：CAS + 桶节点锁，锁粒度更细，结构与 HashMap 统一，并发性能更高。

3. **HashSet 为什么不允许重复元素？**
    - 底层是 HashMap，元素作为 key，HashMap 的 key 天然不允许重复。

4. **fail-fast 和 fail-safe 的区别？**
    - fail-fast：遍历中修改立即抛异常，依赖 `modCount`；
    - fail-safe：允许并发修改，遍历的是副本/弱一致性视图，不抛异常。

5. **Collections.synchronizedMap 和 ConcurrentHashMap 哪个好？**
    - `Collections.synchronizedMap` 是方法级同步锁，锁粒度粗，性能差；
    - `ConcurrentHashMap` 锁粒度细，并发性能高，高并发场景优先选择。

---

需要我把这些内容压缩成一份一页纸的面试速记口诀吗？