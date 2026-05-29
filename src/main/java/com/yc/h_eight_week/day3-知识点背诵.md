# Day3 双指针、滑动窗口、二分查找、堆 核心知识点背诵笔记 ⭐⭐

---

## 一、滑动窗口模板

```java
// 通用模板：求最长/最短子串
int left = 0, right = 0;
while (right < s.length()) {
    char c = s.charAt(right);
    right++;                        // 扩大窗口
    // 更新窗口状态...

    while (窗口需要收缩) {
        char d = s.charAt(left);
        left++;                     // 收缩窗口
        // 更新窗口状态...
    }
    // 更新答案...
}
```

### 1.1 最长无重复子串

```java
Map<Character, Integer> map = new HashMap<>();
int left = 0, maxLen = 0;
for (int right = 0; right < s.length(); right++) {
    char c = s.charAt(right);
    if (map.containsKey(c))
        left = Math.max(left, map.get(c) + 1);  // 跳过重复
    map.put(c, right);
    maxLen = Math.max(maxLen, right - left + 1);
}
```

---

## 二、二分查找

### 2.1 标准二分（左闭右闭）

```java
int l = 0, r = nums.length - 1;
while (l <= r) {
    int mid = l + (r - l) / 2;      // 防溢出
    if (nums[mid] == target) return mid;
    else if (nums[mid] < target) l = mid + 1;
    else r = mid - 1;
}
return -1;
```

### 2.2 查找左边界

```java
// 第一个等于 target 的位置
int l = 0, r = nums.length - 1;
while (l <= r) {
    int mid = l + (r - l) / 2;
    if (nums[mid] >= target) r = mid - 1;
    else l = mid + 1;
}
return (l < nums.length && nums[l] == target) ? l : -1;
```

---

## 三、堆（Top K 模板）

```java
// 第 K 大元素 → 小顶堆
PriorityQueue<Integer> heap = new PriorityQueue<>();
for (int num : nums) {
    heap.offer(num);
    if (heap.size() > k) heap.poll();   // 堆大小保持在 K
}
return heap.peek();

// 第 K 小元素 → 大顶堆
PriorityQueue<Integer> heap = new PriorityQueue<>((a, b) -> b - a);
```

---

## 四、双指针经典题速记

| 题目 | 技巧 |
|---|---|
| 两数之和 II（有序） | 左右指针 → sum 小则 l++，大则 r-- |
| 盛水容器 | 左右指针 → 谁矮移谁 |
| 三数之和 | 排序 + 固定一个 + 左右指针找另外两个（去重） |
| 快慢指针 | 链表中点 / 判环 / 环入口 |

---

## 五、终极背诵总结

1. **滑动窗口**：right 扩窗口 → 满足条件 left 收缩 → 更新答案
2. **二分查找**：左闭右闭 `l<=r`，`mid=l+(r-l)/2` 防溢出
3. **Top K**：第 K 大用小顶堆，第 K 小用大顶堆
4. **双指针**：有序数组左右收，链表快慢判环中点
