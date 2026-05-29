# Day1 数组、链表、栈、队列、哈希 经典算法核心知识点背诵笔记 ⭐⭐

---

## 一、数据结构复杂度速查

| 数据结构 | 随机访问 | 插入 | 删除 | 查找 |
|---|---|---|---|---|
| **数组** | **O(1)** | O(n) | O(n) | O(n) |
| **链表** | O(n) | **O(1)**（已定位） | **O(1)**（已定位） | O(n) |
| **栈** | — | O(1) (push) | O(1) (pop) | O(n) |
| **队列** | — | O(1) (offer) | O(1) (poll) | O(n) |
| **哈希表** | — | O(1) | O(1) | **O(1)** |

---

## 二、数组两大技巧

### 2.1 双指针（左右指针）

```java
// 有序数组 two sum
int l = 0, r = nums.length - 1;
while (l < r) {
    int sum = nums[l] + nums[r];
    if (sum == target) return new int[]{l, r};
    else if (sum < target) l++;
    else r--;
}
```

### 2.2 快慢指针（链表）

```java
// 环形链表检测
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    if (slow == fast) return true;  // 有环
}

// 找环入口：相遇后 1 个回 head，都走 1 步，再次相遇=入口
```

---

## 三、链表必背两题

### 3.1 反转链表

```java
// 迭代
ListNode prev = null, curr = head;
while (curr != null) {
    ListNode next = curr.next;
    curr.next = prev;
    prev = curr;
    curr = next;
}
return prev;

// 递归
if (head == null || head.next == null) return head;
ListNode newHead = reverseList(head.next);
head.next.next = head;
head.next = null;
return newHead;
```

### 3.2 哨兵节点技巧

```java
// 合并两个有序链表 —— 用哨兵简化边界
ListNode dummy = new ListNode(0), curr = dummy;
while (l1 != null && l2 != null) {
    if (l1.val < l2.val) { curr.next = l1; l1 = l1.next; }
    else                 { curr.next = l2; l2 = l2.next; }
    curr = curr.next;
}
curr.next = (l1 != null) ? l1 : l2;  // 剩余接上
return dummy.next;
```

---

## 四、栈 & 队列

### 4.1 Java 最佳实践

```java
// 栈：用 Deque 替代 Stack（Stack 是遗留类，synchronized 性能差）
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);  stack.pop();  stack.peek();

// 队列
Queue<Integer> queue = new LinkedList<>();
queue.offer(1);  queue.poll();  queue.peek();
```

### 4.2 经典题：有效括号

```java
Deque<Character> stack = new ArrayDeque<>();
for (char c : s.toCharArray()) {
    if (c == '(') stack.push(')');
    else if (c == '[') stack.push(']');
    else if (c == '{') stack.push('}');
    else if (stack.isEmpty() || stack.pop() != c) return false;
}
return stack.isEmpty();
```

---

## 五、哈希表

**O(1) 查找的核心**：`HashMap` / `HashSet`

```java
// 两数之和 —— 哈希表 O(n)
Map<Integer, Integer> map = new HashMap<>();
for (int i = 0; i < nums.length; i++) {
    int complement = target - nums[i];
    if (map.containsKey(complement)) return new int[]{map.get(complement), i};
    map.put(nums[i], i);
}
```

---

## 六、终极背诵总结

1. **双指针**：左右指针（有序数组两端收缩）、快慢指针（链表判环）
2. **反转链表**：迭代 pre/curr/next 三变量轮转、递归 head.next.next=head
3. **哨兵节点**：简化链表删除/合并的边界处理
4. **栈用 Deque**：`ArrayDeque` 替代 `Stack`
5. **哈希**：O(1) 查找，空间换时间
