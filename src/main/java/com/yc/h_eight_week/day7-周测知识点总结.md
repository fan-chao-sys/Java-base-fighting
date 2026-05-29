# Day7 模拟面试复盘 —— 全 8 周综合自测

> 本周是终极模拟面试周，10 道题覆盖全部 8 周知识体系。每题都是高频面试真题。

---

## 一、10 道全真模拟面试题

### Q1: HashMap put 流程

**答（Week 2 核心）：**

```
① hash(key) → (h = key.hashCode()) ^ (h >>> 16)    高低位异或减碰撞
② (n-1) & hash 定位桶位                                位运算等价取模
③ 桶空 → 直接 newNode
④ 桶首 key 相等（hash同 && (== 或 equals)）→ 覆盖 value
⑤ 桶首是 TreeNode → 走红黑树插入
⑥ 遍历链表 → 尾插法 → key 同则覆盖，不同尾插
⑦ binCount ≥ 7 → treeifyBin → 数组 ≥ 64 树化，否则扩容
⑧ ++size > threshold → resize（容量翻倍，hash & oldCap 判断去留）
```

**关键参数**：容量 16、负载因子 0.75、树化 8、退化 6、最小树化容量 64。

---

### Q2: synchronized 锁升级过程

**答（Week 3 核心）：**

```
无锁(001) → 偏向锁(101，CAS 写线程ID) → 轻量级锁(00，CAS 自旋) → 重量级锁(10，OS mutex)

无锁 → 第一次线程获取 → Mark Word 写偏向线程 ID
     → 其他线程竞争 → 偏向锁撤销 → 升级轻量级锁
     → 自旋失败/竞争激烈 → 膨胀为重量级锁 → 阻塞，OS 调度
```

**关键**：Mark Word 的最低 3 位决定锁状态。JDK 15+ 默认关闭偏向锁（现代应用竞争多，撤销成本 > 收益）。

---

### Q3: AQS 核心思想

**答（Week 3 核心）：**

> **AQS = volatile int state + CLH 变体 FIFO 队列 + CAS 操作 state**

- **state**：同步状态（0=未锁，1=已锁，>1=重入次数）
- **CLH 队列**：双向链表，每个 Node 为一个等待线程
- **CAS**：无锁修改 state，失败入队自旋/阻塞
- **模板方法**：`acquire()`/`release()` 框架，子类只需实现 `tryAcquire()`/`tryRelease()`

**基于 AQS 的实现**：`ReentrantLock`（state=重入次数）、`Semaphore`（state=许可数）、`CountDownLatch`（state=倒计数）。

---

### Q4: JVM 内存区域和 GC 回收算法

**答（Week 4 核心）：**

**5 大区域**：堆（共享/对象）、方法区/元空间（共享/类元数据）、虚拟机栈（私有/栈帧）、本地方法栈、程序计数器。

**堆结构**：年轻代 Eden:S0:S1=8:1:1（复制算法） + 老年代（标记-清除/整理）。

**GC 算法分配**：
- 年轻代：**复制算法**（对象朝生夕灭，只需复制少量存活对象，无碎片）
- 老年代：**标记-清除**（CMS，有碎片）+ **标记-整理**（Serial Old，无碎片但慢）

**GC 器选型**：大堆 G1（Region 化，Mixed GC，可预测停顿）、超大堆低延迟 ZGC。

---

### Q5: MySQL B+ 树为什么适合做索引

**答（Week 5 核心）：**

| 对比 | B+ Tree 优势 |
|---|---|
| **vs 红黑树** | B+ 树非叶子只存键 → 扇出大 → **树更矮** → 磁盘 IO 更少 |
| **vs B 树** | B 树非叶子也存数据 → 同样高度下索引项少；叶子无链表 → 范围查询慢 |
| **B+ 树独有** | 叶子节点**双向链表** → 范围查询（`BETWEEN`/`>`）定位起点后顺序读取 |

**一句话**：非叶只存键让树更矮（磁盘 IO 少），叶子有链表让范围查询快。

---

### Q6: MVCC 实现原理

**答（Week 5 核心）：**

```
每行隐藏列：
  trx_id（最近修改的事务ID）+ roll_pointer（回滚指针 → undo log 历史版本）

ReadView 可见性判断：
  trx_id < min_trx_id    → ✅ 可见（已提交）
  trx_id ≥ max_trx_id    → ❌ 不可见（将来事务）
  在 [min, max) 且在活跃列表中 → ❌ 不可见，否则 ✅ 可见
  trx_id = creator_trx_id → ✅ 可见（自己的修改）

RC vs RR 核心差异：
  RC：每次 SELECT 生成新 ReadView → 看最新已提交 → 有不可重复读
  RR：事务开始生成一个 ReadView 用到底 → 同事务读一样 → 解决不可重复读
```

**一句话**：trx_id 标身份、roll_pointer 串版本、ReadView 判可见。

---

### Q7: Spring 循环依赖怎么解决

**答（Week 6 核心）：**

**三级缓存**：一级（成品 `singletonObjects`）、二级（半成品 `earlySingletonObjects`）、三级（工厂 `singletonFactories`）。

```
A ↔ B 循环依赖解决流程：
  A 实例化 → 三级缓存放 A 的 ObjectFactory → A 缺 B
  → B 实例化 → B 缺 A → 从三级缓存拿 A 的半成品
  → B 完成 → A 拿到完整 B → A 完成
```

**为什么三级？** 要处理 AOP 代理。A 如果被代理，B 拿到原始 A 还是代理 A？三级存 `ObjectFactory` 可动态决定。

**构造器注入不行**：构造器在实例化阶段就需要依赖 → 还没放入三级缓存 → 无法解决。

---

### Q8: Spring Boot 自动装配机制

**答（Week 6 核心）：**

```
@SpringBootApplication
  → @EnableAutoConfiguration
    → @Import(AutoConfigurationImportSelector.class)
      → 读取 META-INF/spring/AutoConfiguration.imports
        → 按 @Conditional 过滤
          ├── @ConditionalOnClass（classpath 有才加载）
          ├── @ConditionalOnMissingBean（用户没定义才创建）
          └── @ConditionalOnProperty（配置匹配才生效）
            → 加载配置类 → 创建 Bean
```

**一句话**：根据 classpath 的 jar 和配置，按需自动创建 Bean。

---

### Q9: Redis 缓存击穿如何解决

**答（Week 7 核心）：**

**击穿**：热点 key 过期瞬间，大量并发请求直打 DB。

**解决方案**：
1. **互斥锁**（最常用）：`SET NX PX` 加分布式锁 → 只有获得锁的线程去查 DB 重建缓存 → 其他线程自旋等缓存
2. **逻辑永不过期**：缓存不设过期 → 后台异步定时刷新 → 永远不出现"空窗期"

**注意**：这和穿透（布隆过滤器 + 缓存空值）和雪崩（过期加随机值 + 高可用）是不同的，面试中容易混淆。

---

### Q10: CPU 飙高怎么排查（完整流程）

**答（Week 8 核心 + Week 4 JVM 补充）：**

```
① top                          → 找 CPU 最高的 Java 进程 PID
② top -Hp <pid>                → 找进程中 CPU 最高的线程 TID
③ printf '%x\n' <tid>          → 线程 ID 转 16 进制
④ jstack <pid> | grep -A 30 <nid> → 定位线程栈 → 看是哪段代码
⑤ 判断线程类型：
   - GC 线程 → jstat -gcutil 确认 → 调 GC 或排查内存泄漏
   - 业务线程 → 代码热点/死循环 → 修代码
   - VM Thread → jmap dump + MAT 分析
   - 死锁线程 → jstack 直接打印 Found deadlock
```

---

## 二、高频算法题清单（15 题）

| 序号 | 题目 | LeetCode | 解法一句话 |
|---|---|---|---|
| 1 | 反转链表 | #206 | 迭代 pre/curr/next 三轮转；递归 head.next.next=head |
| 2 | 环形链表 | #141 | 快慢指针相遇则有环 |
| 3 | 环形链表 II（找入口） | #142 | 相遇后一个回 head，都走 1 步，再次相遇=入口 |
| 4 | 有效括号 | #20 | 栈匹配括号对 |
| 5 | 两数之和 | #1 | HashMap 存 visited，O(n) |
| 6 | 三数之和 | #15 | 排序 + 定一 + 左右指针 + 去重 |
| 7 | 最长无重复子串 | #3 | HashMap 存最后位置 + 滑动窗口 |
| 8 | 二分查找左/右边界 | #34 | 两次二分：找第一个和最后一个 |
| 9 | 数组第 K 大 | #215 | 小顶堆（size=k），O(nlogk) |
| 10 | 层序遍历 | #102 | BFS 队列 + 每层 size |
| 11 | 最大深度 | #104 | `1 + max(左,右)` |
| 12 | 最近公共祖先 | #236 | 左右都非空→当前是 LCA；否则走非空边 |
| 13 | 爬楼梯 | #70 | dp[i]=dp[i-1]+dp[i-2]，空间压两变量 |
| 14 | 买卖股票最佳时机 | #121 | 每天更新 minPrice + max(profit, price-minPrice) |
| 15 | 最长递增子序列 | #300 | dp[i]=max(dp[j]+1)，j<i && nums[j]<nums[i] |

---

## 三、Linux 排查命令速查表

### 3.1 CPU 排查

| 命令 | 作用 |
|---|---|
| `top -c` | 查看进程 CPU/内存，P=CPU排序 |
| `top -Hp <pid>` | 查看进程中线程 CPU |
| `jstack <pid>` | 线程栈，找 BLOCKED/RUNNABLE/死锁 |
| `jstack <pid> \| grep -A 30 deadlock` | 直接找死锁 |

### 3.2 内存排查

| 命令 | 作用 |
|---|---|
| `jstat -gcutil <pid> 1000` | 每秒 GC 各区域占比，FGC 必须 0 |
| `jmap -histo <pid> \| head -20` | 对象直方图 |
| `jmap -dump:format=b,file=xxx.hprof <pid>` | 堆 dump |
| `free -h` | 系统内存 |

### 3.3 磁盘/IO

| 命令 | 作用 |
|---|---|
| `df -h` | 磁盘使用 |
| `iostat -x 1` | 磁盘 IO 实时 |
| `vmstat 1` | 系统整体（CPU/内存/IO/Swap） |

### 3.4 网络/端口

| 命令 | 作用 |
|---|---|
| `netstat -tlnp` | 监听端口 |
| `lsof -i :8080` | 查谁在用 8080 |
| `ss -tlnp` | 更快的 netstat |

### 3.5 日志

| 命令 | 作用 |
|---|---|
| `tail -f -n 200 app.log` | 实时滚最新 200 行 |
| `grep "ERROR" app.log \| tail -20` | 过滤最新错误 |
| `less app.log` | 大文件（/ 搜索向下, ? 搜索向上） |

### 3.6 Java 专属

| 命令 | 作用 |
|---|---|
| `jps -lvm` | Java 进程+JVM参数 |
| `jinfo <pid>` | JVM 运行参数 |
| `jcmd <pid> help` | 可用命令列表 |
| `jcmd <pid> GC.heap_dump xxx.hprof` | 堆 dump（推荐） |

---

## 四、项目亮点与难点话术

### 4.1 自我介绍模板（1 分钟）

```
"面试官您好，我叫[姓名]，有[X]年 Java 开发经验。
熟悉 Spring Boot + MySQL + Redis + MQ 主流技术栈，
对 JVM 和并发编程有较深的理解，能够独立完成线上排查和性能优化。
最近主导/参与了[项目名称]，实现了[核心功能]，将[指标]从[旧值]优化到[新值]。
我非常注重技术深度和底层原理的积累，坚持系统化学习超过 8 周。
希望有机会加入贵团队。"
```

### 4.2 性能优化案例话术（5 分钟）

```
"我分享一个接口性能优化的案例。
- 现象：订单列表接口 P99 从 200ms 飙升到 2s
- 定位：① EXPLAIN 发现 type=ALL 全表扫描 50 万行
        ② jstat 发现 Full GC 频繁
- 优化：① SQL：建联合索引 (user_id, create_time)，type ALL→range
        ② 加 Redis 缓存，过期时间加随机值防雪崩
        ③ JVM 调大年轻代 -Xmn，减少大对象直入老年代
- 效果：P99 从 2s → 80ms，QPS 从 200 → 800，Full GC 降至 0"
```

### 4.3 故障排查案例话术（5 分钟）

```
"我分享一次线上数据库连接池耗尽的故障。
- 现象：凌晨告警，订单接口大面积超时
- 排查：① CPU 100% → jstack 发现大量线程 BLOCKED 等数据库连接
        ② 连接池配置 maxActive=20，被一条慢 SQL 全占了
        ③ 定位慢 SQL：EXPLAIN 发现全表扫描
- 止血：① 重启服务 + 临时调大连接池 ② 紧急加索引
- 根治：① 所有 SQL 上线前 EXPLAIN 校验
        ② 连接池 maxWait 加超时 + 熔断
        ③ 慢查询 > 500ms 钉钉告警
        ④ 连接池指标接入 Grafana"
```

### 4.4 面试反问话术

```
"请问咱们团队的日常技术栈和架构是怎样的？"
"这个岗位负责的核心模块有哪些？有什么技术挑战？"
"团队在性能优化和故障排查方面有什么沉淀？"
"新人在技术深度成长上有什么支持？"
```

---

## 五、模拟面试复盘检查清单

### 5.1 录音自查标准

| 检查项 | 评分（√/△/✗） |
|---|---|
| 每个问题能在 5 秒内开始回答 | |
| 回答时长控制在 1-3 分钟 | |
| 用了"定义→原理→优缺点→场景→项目实糊"结构 | |
| 没有"嗯/啊/然后"等口头禅 | |
| 卡壳超过 3 秒的地方标记出来 | |
| 每个原理都配了代码/场景佐证 | |

### 5.2 改进点模板

```
改进点 1：[卡壳问题] → [准备对应话术]
改进点 2：[逻辑混乱] → [改用结构化模板重写]
改进点 3：[口头禅频次] → [录音重练 3 遍]
```

### 5.3 面试前 15 分钟热身

```
□ 过一遍 10 道全真模拟题的速记口诀
□ 默念一遍 1 分钟自我介绍
□ 深呼吸 3 次，心态放平
```

---

## 六、全 8 周速记口诀汇总

1. **Java 基础**：值传递传副本，Integer 缓存 -128~127，String 不可变四安全
2. **集合**：HashMap 数组+链表+红黑树，put 八步尾插法，扩容翻倍高低位分流
3. **并发**：锁升级无→偏向→轻量→重，volatile 可见性+有序性不保原子，AQS=state+CLH+CAS
4. **JVM**：堆管对象栈管帧，年轻代复制老标整，G1 Region Mixed GC 可预测停顿
5. **MySQL**：B+ 树非叶只存键叶子链表，聚簇存整行二级存主键要回表，RC 每读新视图 RR 一次看定
6. **Spring**：IOC 管对象 AOP 管方法，三级缓存解循环依赖，自动装配读 imports 按需加载
7. **Redis/MQ**：Redis 内存+单线程+IO 多路复用快，MQ 生产 confirm→Broker 刷盘→消费手动 ACK
8. **Linux 排查**：top 找进程→top -Hp 找线程→jstack 看栈→jstat 看 GC→jmap dump→MAT 分析

---

> **面试密钥**：每道题都按 "定义一句话 → 原理核心点 → 对比/优缺点 → 项目实际场景" 的结构回答，保证稳过！
