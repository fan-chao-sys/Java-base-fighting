# Day7 周测知识点总结 —— MySQL 基础、索引与事务

> 覆盖 Day1~Day6 全部核心知识点，对应"学习任务清单" Day 7 整周复盘要求。

---

## 一、10 道自测题（口述标准答案）

### Q1: 为什么 MySQL 用 B+ 树而不用红黑树/B 树？

**答：**

| | B+ Tree | B Tree | 红黑树 |
|---|---|---|---|
| **非叶子存数据？** | ❌ 只存键 | ✅ 存键+数据 | ✅ 存键+数据 |
| **叶子有链表？** | ✅ 双向链表 | ❌ | ❌ |
| **树高** | **矮**（高扇出） | 中等 | **高**（二叉树） |
| **范围查询** | **极快**（链表顺序遍历） | 需中序回退 | 需中序遍历 |

**核心三点**：
1. **磁盘 IO 少**：非叶子只存键 → 每页存更多索引项 → 树更矮 → 查一次只读 2~4 页
2. **范围查询快**：叶子节点双向链表 → `BETWEEN` / `>` 定位起点后顺序遍历
3. **查询稳定**：每次都落到叶子节点 → O(log n) 固定高度

---

### Q2: 聚簇索引和二级索引的区别？

**答：**

| | 聚簇索引 | 二级索引 |
|---|---|---|
| **叶子存储** | **完整行数据** | 索引键 + **主键值** |
| **数量** | 一张表**只能有 1 个** | 可以有**多个** |
| **生成规则** | 有主键用主键 → 无则第一个 UNIQUE NOT NULL → 都没有就隐式 row_id | 手动创建 |
| **查询路径** | 直接定位到行数据 | 拿到主键 → 还要回表 |

**InnoDB 一定有聚簇索引**，推荐显式建自增主键（避免隐式 row_id 的额外开销和无法查询的问题）。

**本周验证（W5D2_Actual.sql）**：建表 + 二级索引 `idx_username` → EXPLAIN 验证 `Using index condition`（回表）vs `Using index`（覆盖索引不回了）。

---

### Q3: 什么是回表？覆盖索引怎么避免回表？

**答：**

```
SELECT * FROM user WHERE name = '张三';

① idx_name 二级索引查 '张三' → 得到主键 id=101
② 聚簇索引查 id=101 → 取完整行 (id, name, age, email, ...)
                         ↑
                      这就是"回表"，额外一次 B+ 树查找
```

**覆盖索引**：查询列全部在索引中 → **不需要回表**。

```sql
INDEX idx_name_age(name, age)

-- ❌ 回表（需要 email 列，索引里没有）
SELECT * FROM user WHERE name = '张三';

-- ✅ 覆盖索引（name + age 在索引中直接返回）
SELECT name, age FROM user WHERE name = '张三';
-- EXPLAIN Extra: Using index（覆盖索引标志）
```

**ICP（索引下推）**：MySQL 5.6+，在引擎层先用索引中的列过滤，减少回表次数。

---

### Q4: 什么是最左前缀规则？为什么会有？

**答：**

联合索引 `(a, b, c)` 按 **a → b → c** 的顺序构建 B+ 树。先按 a 排序，a 相同再排 b，b 相同再排 c。

**物理结构决定的**：a 的值分散在整个 B+ 树上是全局有序的；但 b 只在 a 相等的区间内局部有序。没有 a，b 在整个树上乱序 → 无法用索引定位。

```
✅ WHERE a=1                   → 用 (a)
✅ WHERE a=1 AND b=2           → 用 (a,b)
✅ WHERE a=1 AND b=2 AND c=3   → 用 (a,b,c)
✅ WHERE a=1 AND c=3           → 用 (a)，c 断了
❌ WHERE b=2                   → 全表扫描（没有带头大哥 a）
❌ WHERE c=3                   → 全表扫描
```

**本周验证（W5D3_Actual.sql）**：订单表 `idx(user_id, order_status, create_time)` + 10 条 SQL，逐一验证最左前缀和索引失效。

---

### Q5: 至少列举 5 种索引失效情况

**答：**

| 序号 | 场景 | 示例 | 原因 |
|---|---|---|---|
| ① | LIKE 以 `%` 开头 | `WHERE name LIKE '%张'` | 无法定位 B+ 树起点 |
| ② | 隐式类型转换 | `WHERE phone = 13800138000`（phone 是 VARCHAR） | MySQL 把字段转数字 |
| ③ | 索引列上使用函数 | `WHERE DATE(create_time) = '2024-01-01'` | 索引是原始值 |
| ④ | 索引列上运算 | `WHERE id + 1 = 10` | 同函数 |
| ⑤ | OR 中有非索引列 | `WHERE name='张' OR age=25`（age 无索引） | 一个不走全不走 |
| ⑥ | 不等于 `!=` / `<>` | `WHERE status != 1` | 扫描范围大 |
| ⑦ | 不满足最左前缀 | `WHERE b=2 AND c=3`（索引 a,b,c） | B+ 树物理结构 |
| ⑧ | 优化器选择全表扫描 | 回表行 > 全表 ~20% | 优化器认为全表更快 |

**口诀**：`%`LIKE 在前，类型转换，函数运算，OR 混非索引，`!=` / 断左 / 优化器放弃。

---

### Q6: 4 种隔离级别各解决了什么并发问题？

**答：**

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---|---|---|---|
| **READ UNCOMMITTED** | ✅ 存在 | ✅ 存在 | ✅ 存在 |
| **READ COMMITTED** | ❌ 解决 | ✅ 存在 | ✅ 存在 |
| **REPEATABLE READ**（InnoDB 默认） | ❌ 解决 | ❌ 解决 | ⚠️ 基本解决 |
| **SERIALIZABLE** | ❌ 解决 | ❌ 解决 | ❌ 解决 |

**实现方式**：
- **RC**：每次 SELECT 生成新 ReadView → 看到最新已提交数据 → 有不可重复读
- **RR**：事务开始生成一个 ReadView 用到底 → 两次读相同快照 → 解决不可重复读
- **RR 防幻读**：快照读靠 MVCC，当前读（`FOR UPDATE`）靠 Next-Key Lock

**本周验证（W5D4_Actual.sql）**：两个事务分别设置 READ UNCOMMITTED / READ COMMITTED / REPEATABLE READ，开两个终端演示脏读、不可重复读、幻读现象。ACID 各由 undo log / 锁+MVCC / redo log 保障。

---

### Q7: MVCC 的实现原理？（ReadView + undo log 版本链）

**答：**

**两隐藏列 + 版本链**：

```
当前版本（trx_id=105，balance=2000）
  │ roll_pointer
  ▼
历史版本1（trx_id=100，balance=1500）
  │ roll_pointer
  ▼
历史版本2（trx_id=90，balance=1000）
  │ roll_pointer → NULL
```

**ReadView 可见性判断**：

```
被访问版本的 trx_id < min_trx_id    → ✅ 可见（修改已提交）
被访问版本的 trx_id ≥ max_trx_id    → ❌ 不可见（将来的事务）
min_trx_id ≤ trx_id < max_trx_id    → 在活跃列表中？❌ 不可见 : ✅ 可见
被访问版本的 trx_id = creator_trx_id → ✅ 可见（自己的修改）
```

**RC vs RR**：
- **RC**：每次 SELECT 生成新 ReadView → 能看到最新已提交
- **RR**：事务开始生成一个 ReadView → 全程同一个快照

**本周验证（W5D5_Actual.sql）**：事务中多次更新同一行 → 手动画出 undo log 版本链 + ReadView 可见性判断逻辑。

---

### Q8: redo log 和 binlog 的区别？

**答：**

| | redo log | binlog |
|---|---|---|
| **层级** | **InnoDB 引擎层** | **MySQL Server 层** |
| **日志类型** | **物理日志**（"某页某偏移量改了什么"） | **逻辑日志**（SQL 语句或行变更） |
| **写入方式** | **循环写**（固定大小，覆盖历史） | **追加写**（保留全部历史） |
| **用途** | **崩溃恢复**（crash recovery） | **主从复制** + 基于时间点恢复 |
| **刷盘控制** | `innodb_flush_log_at_trx_commit` | `sync_binlog` |

**两阶段提交**：`redo log prepare → write binlog → redo log commit`

**为什么需要两阶段提交？** 保证 redo log 和 binlog 一致。如果 redo 写了 binlog 没写 → 主库恢复但从库没有这条记录 → 主从不一致。

**本周验证（W5D5_Actual.sql）**：事务前后执行 `SHOW ENGINE INNODB STATUS` 和 `SHOW MASTER STATUS`，观察 redo log LSN 增长和 binlog Position 变化。

---

### Q9: Next-Key Lock 是什么？解决什么问题？

**答：**

**Next-Key Lock = Record Lock（行锁）+ Gap Lock（间隙锁）**

```
索引记录间的间隙：
  (间隙)    id=5    (间隙)    id=10    (间隙)    id=15
  -∞                                               +∞

WHERE id = 10 FOR UPDATE：
  Next-Key Lock 锁住 (5, 10]  ← 间隙(5,10) + 记录10
  → 防止 INSERT id=6/7/8/9（间隙锁）
  → 防止 UPDATE/DELETE id=10（记录锁）
```

**解决的核心问题**：InnoDB RR 级别下**当前读的幻读**。

- 快照读（普通 SELECT）：MVCC ReadView 解决
- 当前读（SELECT ... FOR UPDATE / UPDATE / DELETE）：Next-Key Lock 锁住间隙，防止其他事务 INSERT

**降级情况**：
- 等值查询命中 → Record Lock
- 等值查询未命中 → Gap Lock
- 范围查询 → Next-Key Lock

---

### Q10: EXPLAIN 中 type 字段各值从好到差怎么排？

**答：**

```
const > eq_ref > ref > range > index > ALL
  │       │       │      │       │      │
主键等值  关联表   普通   索引    全索引   全表
1行     主键匹配  索引   范围    扫描    扫描
                 等值   扫描
```

| type | 含义 | 本周 SQL 验证 |
|---|---|---|
| **const** | 主键等值，最多 1 行 | `WHERE id = 1` |
| **eq_ref** | JOIN 用主键/唯一索引，每行匹配 1 条 | `JOIN ON a.id = b.id` |
| **ref** | 普通索引等值 | `WHERE name = 'A'` |
| **range** | 索引范围扫描 | `WHERE id > 10 AND id < 100` |
| **index** | 全索引扫描 | `SELECT name FROM t` (name 有索引) |
| **ALL** | 全表扫描（**最差，必须优化**） | `WHERE age = 25` (age 无索引) |

**Extra 红灯**：`Using filesort`（额外排序）/ `Using temporary`（临时表）→ 必须优化。

**本周验证（W5D6_Actual.sql）**：5 条业务 SQL EXPLAIN 分析，从 const → ALL 逐一评估优劣。

---

## 二、MySQL 索引设计规范

### 2.1 索引设计核心原则

| 原则 | 说明 | 反面案例 |
|---|---|---|
| **最左前缀匹配** | 联合索引等值在前、范围在后 | `(create_time, user_id)` → 只按时间查，user_id 失效 |
| **区分度高放前面** | 选择性越大越靠前 | 性别放第一列 → 只能过滤 50%，后续列负担重 |
| **覆盖索引优先** | 高频查询列直接包含在索引中 | `SELECT a,b,c` 只建了 `(a)` → 必须回表 |
| **避免过多索引** | 单表索引 ≤ 5 个，单索引列 ≤ 3~5 | 每个列都建单列索引 → 写入慢 + index merge 低效 |
| **自增主键** | 顺序追加，避免页分裂 | UUID 主键 → 插入位置随机 → 频繁页分裂 |
| **字符串前缀索引** | 长 VARCHAR 只索引前缀 | `INDEX idx(name(10))` |

### 2.2 联合索引设计模板

```sql
-- 订单查询场景：最常见查询是 用户ID + 状态 + 时间范围
-- 设计：等值(user_id, status) 在前，范围(create_time) 在后
CREATE INDEX idx_uid_status_time ON orders(user_id, status, create_time);

-- 等价于三个索引：
-- (user_id)
-- (user_id, status)
-- (user_id, status, create_time)
```

**本周验证（W5D3_Actual.sql）**：订单表 `idx(user_id, order_status, create_time)` 完整设计考量注释。

### 2.3 SQL 优化检查清单

```
□ type 是否是 ALL / index？→ 加索引
□ key 是否为 NULL？→ 加索引或改写 SQL
□ Extra 是否有 Using filesort？→ 让 ORDER BY 走索引
□ Extra 是否有 Using temporary？→ 优化 GROUP BY / DISTINCT
□ 是否 SELECT * ？→ 只查需要的列
□ 索引列上是否有函数/运算？→ 改写 SQL
□ 联合索引是否满足最左前缀？→ 调整索引列顺序或查询条件
□ varchar 和数字比较是否加了引号？→ 避免隐式类型转换
```

---

## 三、事务与 MVCC 问答

### 3.1 ACID 机制速查

| 特性 | 实现 | 一句话 |
|---|---|---|
| A 原子性 | **undo log** | 失败后根据 undo log 回滚到修改前 |
| C 一致性 | 约束 + 应用逻辑 | 原子性+隔离性+持久性 综合保障 |
| I 隔离性 | **MVCC + 锁** | MVCC 隔离读写，锁隔离写写 |
| D 持久性 | **redo log（WAL）** | 先写日志再写磁盘，崩溃可重放 |

### 3.2 隔离级别与实现

| 级别 | ReadView 生成策略 | 普通 SELECT | SELECT ... FOR UPDATE |
|---|---|---|---|
| READ UNCOMMITTED | 不使用 | 直接读最新 | 直接读最新 |
| READ COMMITTED | **每次 SELECT 新生成** | MVCC 读已提交 | Record Lock |
| REPEATABLE READ | **事务开始生成一个** | MVCC 读快照 | **Next-Key Lock** |
| SERIALIZABLE | 不使用 | 所有读加 S 锁 | X 锁，串行 |

### 3.3 MVCC 一句话

> 每一行存 trx_id（谁改的）+ roll_pointer（历史版本链），读的时候用 ReadView 判断哪个版本可见。

### 3.4 两阶段提交

```
redo prepare → binlog write → redo commit
    │               │              │
  准备状态       记录SQL变更      确认提交
```

崩溃恢复：
- binlog 完整 + redo prepare → **提交**（补 commit）
- binlog 不完整 → **回滚**

---

## 四、SQL 优化案例总结

### 案例 1：索引失效 —— 函数在列上

```sql
-- ❌ 慢：全表扫描
SELECT * FROM orders WHERE DATE(create_time) = '2026-01-01';

-- ✅ 改写：索引生效
SELECT * FROM orders
WHERE create_time >= '2026-01-01' AND create_time < '2026-01-02';
```

### 案例 2：索引失效 —— 隐式类型转换

```sql
-- phone 列是 VARCHAR(11)
-- ❌ 慢：phone 列被转为数字，索引失效
SELECT * FROM user WHERE phone = 13800138000;

-- ✅ 快：加引号
SELECT * FROM user WHERE phone = '13800138000';
```

### 案例 3：回表过多 —— 覆盖索引

```sql
-- 查询用户姓名和年龄很频繁
-- ❌ 回表：idx(name) → 还需要 age
SELECT name, age FROM user WHERE name = '张三';

-- ✅ 覆盖：建联合索引
CREATE INDEX idx_name_age ON user(name, age);
-- 现在 EXPLAIN Extra: Using index，不需要回表
```

### 案例 4：分页优化

```sql
-- ❌ 慢：大 offset 需要扫描并丢弃前 100000 行
SELECT * FROM orders WHERE user_id = 1 ORDER BY id LIMIT 100000, 20;

-- ✅ 快：基于上次的 id 做范围查询（游标分页）
SELECT * FROM orders WHERE user_id = 1 AND id > 100000 ORDER BY id LIMIT 20;
```

### 案例 5：死锁 —— 加锁顺序不一致

```sql
-- 事务A：先锁 A 再锁 B
UPDATE account SET balance = balance - 100 WHERE name = 'A';  -- 持有 A 锁
UPDATE account SET balance = balance + 100 WHERE name = 'B';  -- 等待 B 锁

-- 事务B：先锁 B 再锁 A
UPDATE account SET balance = balance - 100 WHERE name = 'B';  -- 持有 B 锁
UPDATE account SET balance = balance + 100 WHERE name = 'A';  -- 等待 A 锁 → 死锁！
```

**解决**：统一加锁顺序。`SHOW ENGINE INNODB STATUS` 查看 `LATEST DETECTED DEADLOCK`。

**本周验证（W5D6_Actual.sql）**：两个事务交错加锁 → 触发死锁 → InnoDB 自动回滚代价小的。

---

## 五、本周重点代码清单

| 序号 | 代码 | 文件 | 关键点 |
|---|---|---|---|
| 1 | InnoDB vs MyISAM COUNT 对比 | W5D1_Actual.sql | MyISAM O(1) vs InnoDB O(n) + SHOW ENGINE STATUS |
| 2 | 回表 vs 覆盖索引 EXPLAIN | W5D2_Actual.sql | Using index condition vs Using index |
| 3 | 10 条 SQL 验证规则 | W5D3_Actual.sql | 全等/断左/范围截断/函数失效/排序/覆盖 |
| 4 | 三种并发问题演示 | W5D4_Actual.sql | 脏读(RU)/不可重复读(RC)/幻读(RR) |
| 5 | redo/binlog 观察 + MVCC 链 | W5D5_Actual.sql | SHOW MASTER STATUS + 版本链图 |
| 6 | 死锁 + 5 SQL EXPLAIN | W5D6_Actual.sql | SHOW ENGINE STATUS + const~ALL 评估 |

---

## 六、速记口诀

1. **引擎选型**：InnoDB 事务行锁 MVCC，MyISAM 快读无事务仅表锁
2. **B+ 树三优势**：非叶只存键树矮、叶子存数据查询稳、双向链表范围快
3. **聚簇 vs 二级**：聚簇叶子存整行只一个、二级叶子存主键要回表
4. **最左前缀**：带头大哥不能死，中间兄弟不能断，范围之后全失效
5. **索引失效 5 必背**：LIKE %头 / 类型转换 / 函数运算 / OR 非索引 / 断最左
6. **隔离级别**：RU 啥都看（脏读），RC 只看提交（不可重复），RR 只看快照（默认）
7. **MVCC 三件套**：trx_id 标身份、roll_pointer 串版本、ReadView 判可见
8. **redo vs binlog**：redo 物理循环崩溃恢复，binlog 逻辑追加主从复制
9. **两阶段提交**：redo prepare → binlog → redo commit，保证双日志一致
10. **EXPLAIN**：type 看 const~ALL，Extra 看 filesort/temporary 红灯
