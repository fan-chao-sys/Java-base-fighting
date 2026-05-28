# Day6 锁机制、行锁、间隙锁、Next-Key Lock、死锁与 explain 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、InnoDB 锁类型总览

### 1.1 锁粒度

| 锁类型 | 粒度 | 加锁方式 | 并发度 |
|---|---|---|---|
| **表锁** | 整张表 | `LOCK TABLES ... READ/WRITE` | 低 |
| **行锁** | 单行记录 | 索引记录上加锁 | **高** |
| **页锁** | 一个数据页 | BDB 引擎使用，InnoDB 没有 | 中 |

### 1.2 共享锁 vs 排他锁

| | 共享锁（S 锁，读锁） | 排他锁（X 锁，写锁） |
|---|---|---|
| **加锁方式** | `SELECT ... LOCK IN SHARE MODE` | `SELECT ... FOR UPDATE` |
| **兼容 S 锁** | ✅ | ❌ |
| **兼容 X 锁** | ❌ | ❌ |
| **使用场景** | 确认数据存在、防止更新 | 更新数据、删数据 |

```
兼容矩阵：
      S锁   X锁
S锁   ✅    ❌
X锁   ❌    ❌
```

---

## 二、行锁的三种形式（RR 级别专用）⭐⭐⭐

### 2.1 三种锁的定义

```
索引记录间的间隙：
    (间隙)    记录1    (间隙)    记录2    (间隙)    记录3
   ↑       ↑        ↑       ↑        ↑       ↑        ↑
  -∞       5        6       10       11      15       +∞
```

| 锁类型 | 锁住范围 | 作用 |
|---|---|---|
| **Record Lock**（记录锁） | 锁住**单条索引记录** | 防止该行被修改/删除 |
| **Gap Lock**（间隙锁） | 锁住索引记录**之间的间隙**（不含记录本身） | 防止 INSERT，解决幻读 |
| **Next-Key Lock**（临键锁） | **= Record Lock + 前面的 Gap Lock** | InnoDB RR 的默认行锁，解决当前读幻读 |

### 2.2 Next-Key Lock 示例

```sql
-- 假设 id 索引有值：5, 10, 15
SELECT * FROM t WHERE id = 10 FOR UPDATE;
-- Next-Key Lock 锁住：(5, 10] ← 间隙(5,10) + 记录10
-- 间隙锁阻止 INSERT id=6/7/8/9，记录锁阻止 UPDATE id=10
```

### 2.3 各种 WHERE 条件的锁范围

```sql
-- 等值查询（命中）：退化为 Record Lock
WHERE id = 10  →  锁住 id=10 这条记录

-- 等值查询（未命中）：退化为 Gap Lock
WHERE id = 7   →  锁住 (5, 10) 间隙，防止 id=7 被插入

-- 范围查询：Next-Key Lock
WHERE id >= 10 AND id < 15  →  锁住 [10, 15] + 间隙
```

---

## 三、死锁

### 3.1 死锁四大条件

```
① 互斥：资源只能被一个事务持有
② 持有并等待：事务已持有一部分资源，在等另一部分
③ 不可剥夺：已持有的资源不能被强制拿掉
④ 循环等待：事务A等事务B，事务B等事务A → 形成环
```

### 3.2 死锁排查

```sql
-- 1. 查看死锁信息
SHOW ENGINE INNODB STATUS\G
-- 找到 "LATEST DETECTED DEADLOCK" 段
-- 看 WAITING FOR THIS LOCK TO BE GRANTED
-- 分析等待链

-- 2. 查看当前锁等待
SELECT * FROM information_schema.INNODB_LOCKS;        -- MySQL 5.7
SELECT * FROM performance_schema.data_locks;          -- MySQL 8.0
SELECT * FROM performance_schema.data_lock_waits;     -- 锁等待关系
```

### 3.3 避免死锁

| 方法 | 说明 |
|---|---|
| **相同顺序访问资源** | 所有事务按相同顺序操作表/行 |
| **缩短事务** | 逻辑尽量在事务外完成，事务内只做读写 |
| **合理使用索引** | 索引能减少锁范围（全表扫描→所有行都上锁） |
| **降低隔离级别** | 从 RR 降到 RC（RC 没有间隙锁，减少锁冲突） |
| **NOWAIT / SKIP LOCKED** | MySQL 8.0+，拿不到锁立即返回或跳过已锁行 |

### 3.4 InnoDB 死锁检测

- `innodb_deadlock_detect=ON`（默认）→ 自动检测
- 检测到死锁 → 回滚**代价最小**的事务（undo log 行数最少）
- 高并发下死锁检测本身消耗 CPU → 可考虑关闭 + `innodb_lock_wait_timeout`

---

## 四、EXPLAIN 实战速查

### 4.1 type 字段（从好到差）

```
const > eq_ref > ref > range > index > ALL
  │       │       │      │       │      │
 主键    关联     普通    索引    全索引  全表
 等值    主键     索引    范围    扫描    扫描
```

### 4.2 Extra 关注重点

| Extra | 含义 | 建议 |
|---|---|---|
| **Using index** | 覆盖索引，最优 | ✅ 完美 |
| **Using index condition** | ICP 索引下推 | ✅ 不错 |
| **Using where** | Server 层过滤 | ⚠️ 检查是否可以走索引 |
| **Using filesort** | 额外排序 | ❌ 需优化 |
| **Using temporary** | 用了临时表 | ❌ 必须优化 |
| **Using join buffer** | join buffer 不够 | ⚠️ 调整参数或优化 SQL |

### 4.3 key_len 含义

`key_len` = 使用的索引长度（字节），可判断**用到了联合索引的几列**。

```sql
INDEX idx(a int, b varchar(50))
-- key_len=4   → 只用了 a
-- key_len=206 → 用了 a + b（4 + 50*3+2）
```

### 4.4 EXPLAIN 分析流程

```
① type 有没有 ALL（全表扫描）？ → 优化为 index/range 以上
② key 是否为 NULL？ → 没有用索引 → 加索引或改写 SQL
③ Extra 有没有 Using filesort / Using temporary？ → 修改 SQL 让排序走索引
④ rows 是否合理？ → 扫描行数是否接近实际返回行数
⑤ key_len 是否完整？ → 联合索引是否用全
```

---

## 五、MySQL 8.0 EXPLAIN ANALYZE

```sql
-- 实际执行并返回每步耗时（比 EXPLAIN 更精确）
EXPLAIN ANALYZE SELECT * FROM user WHERE name = '张三';
-- 输出：实际执行时间 + 循环次数 + 实际返回行数
```

---

## 六、SQL 优化思路总结

```
慢查询定位：
  ① 开启慢查询日志 slow_query_log
  ② 找到执行慢的 SQL
  ③ EXPLAIN 分析执行计划

优化三板斧：
  ① 加索引（最左前缀 + 覆盖索引）
  ② 改写 SQL（避免 SELECT * / 避免函数/运算在索引列上）
  ③ 优化表结构（选择合适数据类型、控制索引数量）
```

---

## 七、终极背诵总结

1. **S 锁 vs X 锁**：S 锁兼容 S 不兼容 X，X 锁全不兼容
2. **Next-Key Lock** = Record Lock + Gap Lock，InnoDB RR 下默认行锁
3. **Gap Lock 作用**：锁住间隙，防止 INSERT，解决当前读幻读
4. **死锁四条件**：互斥 + 持有等待 + 不可剥夺 + 循环等待
5. **避免死锁**：固定顺序访问 + 短事务 + 合理索引 + NOWAIT/SKIP LOCKED
6. **EXPLAIN type 排序**：const → eq_ref → ref → range → index → ALL
7. **Using filesort / Using temporary** = 红灯，必须优化
8. **索引下推(ICP)**：引擎层先过滤再回表，Extra=Using index condition
