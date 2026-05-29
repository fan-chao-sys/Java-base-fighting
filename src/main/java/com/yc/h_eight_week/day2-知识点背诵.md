# Day2 二叉树、DFS、BFS、递归套路 核心知识点背诵笔记 ⭐⭐

---

## 一、二叉树遍历模板

### 1.1 递归三板斧

```java
// 前序：根→左→右
void preorder(TreeNode root) {
    if (root == null) return;
    visit(root);
    preorder(root.left);
    preorder(root.right);
}

// 中序：左→根→右
void inorder(TreeNode root) {
    if (root == null) return;
    inorder(root.left);
    visit(root);
    inorder(root.right);
}

// 后序：左→右→根
void postorder(TreeNode root) {
    if (root == null) return;
    postorder(root.left);
    postorder(root.right);
    visit(root);
}
```

### 1.2 迭代版（用栈模拟）

```java
// 中序迭代（最常考）
Deque<TreeNode> stack = new ArrayDeque<>();
TreeNode curr = root;
while (curr != null || !stack.isEmpty()) {
    while (curr != null) {
        stack.push(curr);
        curr = curr.left;           // 一路往左
    }
    curr = stack.pop();
    visit(curr);                    // 访问根
    curr = curr.right;              // 转向右
}
```

---

## 二、BFS 层次遍历

```java
// 队列逐层处理 —— 记录每层 size
Queue<TreeNode> q = new LinkedList<>();
q.offer(root);
while (!q.isEmpty()) {
    int size = q.size();            // 当前层有几个节点
    for (int i = 0; i < size; i++) {
        TreeNode node = q.poll();
        // 处理当前节点
        if (node.left != null)  q.offer(node.left);
        if (node.right != null) q.offer(node.right);
    }
    // 一层处理完毕
}
```

---

## 三、递归解题模板

```
① 明确函数定义：f(root) 做什么？返回什么？
② 确定终止条件：root == null 时怎么办？
③ 把问题分解：f(root) 和 f(root.left)、f(root.right) 的关系？
④ 合并结果返回
```

### 3.1 二叉树最大深度

```java
int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```

### 3.2 最近公共祖先（LCA）

```java
TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    if (left != null && right != null) return root;  // 左右各一个 → 当前是 LCA
    return left != null ? left : right;               // 都在一边
}
```

---

## 四、经典题速记

| 题目 | 思路关键点 |
|---|---|
| 前/中/后序遍历 | 递归模板 / 迭代用栈 |
| 层序遍历 | 队列 BFS + 记录每层 size |
| 最大深度 | `1 + max(左, 右)` |
| 最小深度 | 注意空子树不算深度 |
| 对称二叉树 | 两指针镜像比较 |
| 翻转二叉树 | `swap(left, right)` + 递归 |
| 最近公共祖先 | 左右递归，都非空则当前为 LCA |

---

## 五、终极背诵总结

1. **三序遍历**：递归一行行，迭代用栈记左链
2. **BFS 层次遍历**：队列 + 每层 size 控制
3. **递归模板四步**：定义 → 终止 → 分解 → 合并
4. **LCA**：两边都有→返回当前，只有一边→返回那一边
