# ArrayList 底层原理与核心知识点背诵笔记

---

## 一、核心底层结构

### 1. 底层存储结构
- **底层实现**：基于动态数组实现，核心存储变量为 `transient Object[] elementData`
- **默认容量**：
  - JDK 7+ 采用**懒加载机制**：初始创建时不直接分配长度为10的数组，而是使用空数组 `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`
  - 首次调用 `add()` 方法时，才初始化容量为 `DEFAULT_CAPACITY = 10` 的数组
- **核心特性**：数组的连续性内存布局，决定了 ArrayList 的随机访问优势

---

## 二、扩容机制（核心考点）

### 1. 触发时机
当调用 `add(E e)` / `add(int index, E e)` 方法时，若当前元素个数 `size + 新增元素个数 > elementData.length`，触发扩容。

### 2. 扩容核心流程
1.  **调用 `grow(int minCapacity)` 方法**：
    - 计算新容量：`newCapacity = oldCapacity + (oldCapacity >> 1)`（即旧容量的1.5倍）
    - 特殊场景：
      - 首次扩容：旧容量为0 → 新容量直接设为 `DEFAULT_CAPACITY = 10`
      - 1.5倍扩容后仍不足 `minCapacity`：直接取 `minCapacity` 为新容量
      - 新容量超过 `Integer.MAX_VALUE - 8`：调用 `hugeCapacity()` 处理（最大容量为 `Integer.MAX_VALUE`）
2.  **调用 `Arrays.copyOf(elementData, newCapacity)`**：
    - 底层依赖 `System.arraycopy()` native 方法，将旧数组元素拷贝到新数组
    - 新数组长度为 `newCapacity`，旧数组被GC回收

### 3. 扩容性能影响
- 时间复杂度：最坏情况为 O(n)（数组拷贝耗时）
- 空间复杂度：每次扩容会额外预留 50% 空间，减少后续扩容次数，但会产生内存冗余

---

## 三、核心操作的时间复杂度

| 操作类型                | 时间复杂度 | 底层原理说明                                                                 |
|-------------------------|------------|------------------------------------------------------------------------------|
| `get(int index)`        | O(1)       | 基于数组下标直接定位 `elementData[index]`，无需遍历                           |
| `add(E e)`（尾部添加）  | 均摊 O(1)  | 不扩容时直接赋值；扩容时需数组拷贝（O(n)），但扩容概率低，均摊为O(1)         |
| `add(int index, E e)`   | O(n)       | 需调用 `System.arraycopy()` 将 `index` 及之后的元素后移一位，移动元素个数为 `size - index` |
| `remove(int index)`     | O(n)       | 需调用 `System.arraycopy()` 将 `index` 之后的元素前移一位，移动元素个数为 `size - index - 1` |
| `remove(Object o)`      | O(n)       | 需先遍历找到元素下标（O(n)），再执行删除（O(n)），整体时间复杂度为O(n)        |

---

## 四、fail-fast 机制与 modCount

### 1. 核心变量 `modCount`
- 定义：`protected transient int modCount = 0;`
- 作用：记录 ArrayList 结构修改的次数（包括 `add()`、`remove()`、`clear()` 等会改变数组长度/元素位置的操作）

### 2. fail-fast 触发条件
当使用迭代器 `Iterator` 或 `ListIterator` 遍历 ArrayList 时：
1.  迭代器初始化时，会记录当前的 `modCount` 到 `expectedModCount`
2.  遍历过程中，每次调用 `next()` / `remove()` 方法前，会校验 `modCount == expectedModCount`
3.  若不相等，直接抛出 `ConcurrentModificationException`

### 3. 常见触发场景
- 单线程中：遍历 ArrayList 时，直接调用 `list.add()` / `list.remove()` 方法（未通过迭代器的 `remove()`）
- 多线程中：一个线程遍历，另一个线程修改 ArrayList 结构

### 4. 如何避免 fail-fast
- 单线程：使用迭代器的 `remove()` 方法（会同步更新 `expectedModCount`）
- 多线程：使用 `CopyOnWriteArrayList`（线程安全，采用 fail-safe 机制）

---

## 五、关键特性与底层细节

### 1. 懒加载（JDK 7+）
- 初始创建 `new ArrayList<>()` 时，`elementData` 指向空数组，不分配内存
- 优势：避免创建空 ArrayList 时的内存浪费，只有首次添加元素时才初始化容量

### 2. `System.arraycopy()` vs `Arrays.copyOf()`

| 方法                | 特点说明                                                                 |
|---------------------|--------------------------------------------------------------------------|
| `System.arraycopy()`| native 方法，直接操作内存，可指定源数组、起始位置、目标数组、目标位置、拷贝长度 |
| `Arrays.copyOf()`   | 封装了 `System.arraycopy()`，内部会创建新数组并拷贝元素，常用于扩容场景     |

### 3. 序列化相关
- `elementData` 被 `transient` 修饰，不参与默认序列化
- ArrayList 自定义了 `writeObject()` / `readObject()` 方法，仅序列化数组中实际存储的元素（避免序列化空元素造成空间浪费）

---

## 六、高频面试题总结
1.  **ArrayList 的扩容过程？**
    答：调用 `add()` 时判断是否需要扩容，需要则调用 `grow()`，计算1.5倍新容量，再通过 `Arrays.copyOf()` 拷贝元素到新数组。
2.  **为什么 ArrayList 适合尾部添加和随机访问，不适合中间插入/删除？**
    答：底层是数组，随机访问直接通过下标定位（O(1)）；中间插入/删除需要移动后续元素（O(n)），效率低。
3.  **modCount 作用是什么？fail-fast 机制原理？**
    答：记录结构修改次数，迭代器遍历时校验 modCount，若不一致则抛出并发修改异常，防止并发修改导致的数据不一致。
4.  **ArrayList 与 LinkedList 的区别？**
    答：ArrayList 基于数组，随机访问快、中间增删慢；LinkedList 基于双向链表，随机访问慢、首尾增删快。
5.  **JDK 7+ 中 ArrayList 的懒加载是什么？**
    答：创建 ArrayList 时不初始化容量为10的数组，首次 add 时才初始化，减少空集合的内存占用。
