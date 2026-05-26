# Java集合体系 核心知识点背诵笔记
---
## 一、Java集合框架整体体系
### 核心结构
Java集合框架分为两大独立分支：
1.  **`Collection` 接口**：存储单个元素的顶层接口，包含三大子接口：
    - `List`：有序、可重复集合
    - `Set`：不可重复集合
    - `Queue`：队列（先进先出/优先级队列等）
2.  **`Map` 接口**：存储键值对（Key-Value）的顶层接口，Key不可重复

---
## 二、`List` 接口（有序、可重复、支持索引访问）
### 1. 核心特性
- 元素**有序**：按插入顺序排列，可通过索引直接访问元素
- 元素**可重复**：允许存入重复元素
- 支持索引操作：`get(int index)`、`add(int index, E element)`、`remove(int index)`

### 2. 常用实现类对比
| 实现类 | 底层结构 | 线程安全 | 特点与适用场景 |
|--------|----------|----------|----------------|
| `ArrayList` | 动态数组（`Object[]`） | 不安全 | 随机访问快、增删慢（中间/头部增删需移动元素），默认初始容量10，扩容为1.5倍，适合读多写少场景 |
| `LinkedList` | 双向链表（JDK1.6前为循环链表，JDK1.7+取消循环） | 不安全 | 增删快（仅修改节点指针）、随机访问慢，同时实现`Deque`接口，可作为栈/队列/双端队列使用 |
| `Vector` | 动态数组 | 安全（所有方法`synchronized`） | 扩容为2倍，性能较差，已基本被`ArrayList`替代 |

### 3. 底层关键原理
- `ArrayList` 扩容机制：
  1.  调用`add()`时，先判断是否需要扩容
  2.  计算所需容量（当前元素数+1），若超过数组长度则触发扩容
  3.  新容量 = 旧容量 + 旧容量/2（即1.5倍扩容），通过`Arrays.copyOf()`复制原数组元素到新数组
- `LinkedList` 节点结构：`Node<E> {E item; Node<E> next; Node<E> prev;}`，每个节点维护前驱和后继指针

---
## 三、`Set` 接口（不可重复集合）
### 1. 核心特性
- 元素**不可重复**：依赖`equals()`和`hashCode()`保证唯一性
- 无序性：部分实现类可保证有序（如`LinkedHashSet`）或排序（如`TreeSet`）

### 2. 常用实现类对比
| 实现类 | 底层结构 | 有序性 | 去重原理 | 适用场景 |
|--------|----------|--------|----------|----------|
| `HashSet` | `HashMap`（底层基于`transient HashMap<E,Object> map`） | 无序 | 依赖元素的`hashCode()`和`equals()`方法：<br>1. 计算`hashCode`定位哈希桶<br>2. 桶内无元素则直接存入<br>3. 桶内有元素则遍历链表/红黑树，调用`equals()`判断是否重复 | 去重且不关心顺序的场景 |
| `LinkedHashSet` | `LinkedHashMap`（继承`HashSet`，底层基于链表+哈希表） | 插入有序 | 同`HashSet`，额外维护双向链表记录插入顺序 | 需去重且保留插入顺序的场景 |
| `TreeSet` | `TreeMap`（底层红黑树） | 自然排序/自定义排序 | 依赖`Comparable`（元素自身实现）或`Comparator`（外部比较器）：<br>调用`compareTo()`/`compare()`方法判断元素是否重复（返回0视为重复） | 需去重且按规则排序的场景 |

---
## 四、`Map` 接口（键值对集合，Key不可重复）
### 1. 核心特性
- 存储结构：Key-Value键值对，Key不可重复，Value可重复
- Key唯一性：依赖Key的`equals()`和`hashCode()`保证（或比较器判断）
- 无序性：部分实现类可保证有序（如`LinkedHashMap`）或排序（如`TreeMap`）

### 2. 常用实现类对比
| 实现类 | 底层结构 | 有序性 | 线程安全 | 核心特点 |
|--------|----------|--------|----------|----------|
| `HashMap` | JDK1.7：数组+链表<br>JDK1.8+：数组+链表/红黑树 | 无序 | 不安全 | 初始容量16，负载因子0.75，扩容为2倍；JDK1.8当链表长度≥8且数组长度≥64时，链表转为红黑树优化查询效率 |
| `LinkedHashMap` | 数组+链表/红黑树 + 双向链表 | 插入有序/访问有序（LRU可实现） | 不安全 | 继承`HashMap`，额外维护双向链表记录插入/访问顺序，可用于实现LRU缓存 |
| `TreeMap` | 红黑树（自平衡二叉查找树） | 按Key自然排序/自定义排序 | 不安全 | Key需实现`Comparable`或传入`Comparator`，基于比较器规则排序 |
| `Hashtable` | 数组+链表 | 无序 | 安全（所有方法`synchronized`） | 不允许Key/Value为`null`，性能差，已被`HashMap`+`Collections.synchronizedMap()`或`ConcurrentHashMap`替代 |

### 3. `HashMap` 底层核心原理（高频考点）
1.  **数据结构演进**：
    - JDK1.7：数组+链表，哈希冲突时采用头插法，并发场景下易出现环形链表导致死循环
    - JDK1.8+：数组+链表/红黑树，哈希冲突时采用尾插法，链表长度≥8且数组长度≥64时转为红黑树，优化查询效率（O(n)→O(logn)）
2.  **扩容机制**：
    - 初始容量16，负载因子0.75，当元素数 > 容量×负载因子时触发扩容
    - 新容量为旧容量的2倍，扩容时重新计算哈希值（`hash & (newCap - 1)`），减少哈希冲突
3.  **哈希计算**：`(key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)`，将哈希值的高16位与低16位异或，减少哈希冲突

---
## 五、`Comparable` vs `Comparator` 排序接口
### 1. 核心定义与区别
| 接口 | 别称 | 方法 | 特点 | 排序优先级 |
|------|------|------|------|------------|
| `Comparable` | 内部比较器 | `int compareTo(T o)` | 定义在元素类内部，侵入式实现，只能定义一种排序规则 | 低（被`Comparator`覆盖） |
| `Comparator` | 外部比较器 | `int compare(T o1, T o2)` | 非侵入式实现，可定义多种排序规则，灵活切换 | 高 |

### 2. 核心规则
- `compareTo(a, b)`/`compare(a, b)` 返回值规则：
  - 返回负数：`a` < `b`（升序排列时，`a`排在`b`前面）
  - 返回0：`a` == `b`（在`Set`/`Map`中视为重复元素/Key）
  - 返回正数：`a` > `b`（升序排列时，`a`排在`b`后面）
- 优先级：当同时存在`Comparable`和`Comparator`时，以`Comparator`的规则为准

### 3. 代码示例
```java
// 1. Comparable 内部排序（侵入式）
class User implements Comparable<User> {
    private int age;
    private String name;
    // 按age升序排序
    @Override
    public int compareTo(User o) {
        return this.age - o.age;
    }
}

// 2. Comparator 外部排序（非侵入式）
List<User> users = new ArrayList<>();
// 按name长度降序排序
Collections.sort(users, new Comparator<User>() {
    @Override
    public int compare(User u1, User u2) {
        return u2.getName().length() - u1.getName().length();
    }
});
// Lambda简化写法
Collections.sort(users, (u1, u2) -> u2.getName().length() - u1.getName().length());
