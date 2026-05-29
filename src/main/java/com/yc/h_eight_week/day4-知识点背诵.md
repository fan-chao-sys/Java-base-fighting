# Day4 动态规划入门 核心知识点背诵笔记 ⭐⭐

---

## 一、DP 四步解题法

```
① 定义状态：dp[i] 代表什么？
② 找状态转移方程：dp[i] 和之前状态的关系？
③ 初始化边界条件：dp[0]、dp[1]？
④ 确定遍历方向和输出：从前往后？返回 dp[n] 还是 dp 中最大？
```

---

## 二、经典 DP 题

### 2.1 爬楼梯（斐波那契）

```java
// dp[i] = dp[i-1] + dp[i-2] —— 到第 i 阶 = 从 i-1 跨 1 步 + 从 i-2 跨 2 步
int climbStairs(int n) {
    if (n <= 2) return n;
    int a = 1, b = 2;              // 空间压缩：只用两个变量
    for (int i = 3; i <= n; i++) {
        int c = a + b;
        a = b;
        b = c;
    }
    return b;
}
```

### 2.2 买卖股票最佳时机（单次交易）

```java
int maxProfit(int[] prices) {
    int minPrice = Integer.MAX_VALUE;  // 到目前的最低价格
    int maxProfit = 0;
    for (int price : prices) {
        minPrice = Math.min(minPrice, price);
        maxProfit = Math.max(maxProfit, price - minPrice);
    }
    return maxProfit;
}
```

### 2.3 最长递增子序列（LIS）

```java
// dp[i] = 以 nums[i] 结尾的最长递增子序列长度
// dp[i] = max(dp[j] + 1) 其中 j < i 且 nums[j] < nums[i]
int lengthOfLIS(int[] nums) {
    int[] dp = new int[nums.length];
    Arrays.fill(dp, 1);
    int max = 1;
    for (int i = 1; i < nums.length; i++) {
        for (int j = 0; j < i; j++) {
            if (nums[j] < nums[i])
                dp[i] = Math.max(dp[i], dp[j] + 1);
        }
        max = Math.max(max, dp[i]);
    }
    return max;
}
```

---

## 三、DP 分类速查

| 类型 | 特点 | 代表题 |
|---|---|---|
| **线性 DP** | dp[i] 依赖 dp[i-1] 等 | 爬楼梯、最大子序和 |
| **背包 DP** | 物品选与不选 | 01 背包、完全背包 |
| **区间 DP** | 小区间合并为大区间 | 回文子串 |
| **状态机 DP** | 有多个状态转移 | 买卖股票系列 |
| **树形 DP** | 在树上 DP | 打家劫舍 III |

---

## 四、空间压缩技巧

```java
// 许多 DP 每个状态只依赖前一个 → dp[i] 可压成变量
// 爬楼梯：dp 数组 → 两个变量 a, b 滚动
// 背包：dp[i][j] → dp[j]（倒序遍历 j）
```

---

## 五、终极背诵总结

1. **DP 四步**：定状态 → 找方程 → 初始化 → 定方向
2. **三个经典**：爬楼梯（dp[i]=dp[i-1]+dp[i-2]）、买股票（每天更新 minPrice+maxProfit）、LIS（找前面更小的）
3. **空间压缩**：只依赖前几个状态的 → 用变量替代数组
4. **什么用 DP**：求最值/求方案数/求可行性 + 子问题重叠
