# Day4 事务 ACID、隔离级别、脏读/不可重复读/幻读 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、ACID 四大特性及实现机制

| 特性 | 含义 | 实现机制 | 一句话 |
|---|---|---|---|
| **A 原子性** | 事务要么全部成功，要么全部失败 | **undo log**（回滚日志） | 失败时根据 undo log 回滚到修改前状态 |
| **C 一致性** | 事务前后数据满足所有约束 | 约束（主键/外键/唯一/CHECK）+ 应用层逻辑 | 原子性+隔离性+持久性共同保障 |
| **I 隔离性** | 并发事务互不干扰 | **锁（LBCC）+ MVCC** | 锁隔离写，MVCC 隔离读 |
| **D 持久性** | 事务提交后数据不丢失 | **redo log**（重做日志，WAL策略） | 先写日志再写磁盘，崩溃后可重放恢复 |

### 1.1 各机制的职责

```
undo log  → 记录"修改前的值"   → 回滚 + MVCC 读旧版本  (逻辑日志)
redo log  → 记录"修改了什么"   → 崩溃恢复，保证持久性    (物理日志)
binlog    → 记录"SQL 语句"     → 主从复制、数据恢复       (逻辑日志)
锁        → 隔离写-写冲突      → 悲观并发控制 (LBCC)
MVCC     → 隔离读-写冲突      → 乐观并发控制（读不阻塞写）
```

---

## 二、4 种隔离级别

### 2.1 级别总览

```
隔离级别从低到高：
READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE
      │                  │                │                │
   脏读都可能        不可重复读可能        幻读可能           全部解决
   (最低)                              (InnoDB默认)       (最高/最低性能)
```

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 实现 |
|---|---|---|---|---|
| **READ UNCOMMITTED** | ✅ | ✅ | ✅ | 读不加锁，直接读最新 |
| **READ COMMITTED** | ❌ | ✅ | ✅ | 每次读生成新 ReadView |
| **REPEATABLE READ**（InnoDB 默认） | ❌ | ❌ | ❌\* | 事务开始生成一个 ReadView + Next-Key Lock |
| **SERIALIZABLE** | ❌ | ❌ | ❌ | 所有读都加共享锁，串行执行 |

\* InnoDB RR 通过 Next-Key Lock 解决当前读的幻读

### 2.2 隔离级别怎么选

| 数据库 | 默认级别 | 适用场景 |
|---|---|---|
| **MySQL InnoDB** | REPEATABLE READ | OLTP 通用 |
| **Oracle** | READ COMMITTED | OLTP 通用 |
| 高一致性需求 | SERIALIZABLE | 金融/账务（性能最差） |

---

## 三、3 种并发问题图解

### 3.1 脏读（Dirty Read）

```
事务 A：UPDATE account SET balance = 200 WHERE id = 1  -- 未提交
事务 B：SELECT balance FROM account WHERE id = 1        -- 读到 200
事务 A：ROLLBACK                                        -- 实际还是 100
事务 B：使用的是 200（脏数据），基于它做业务 → 错误！
```

**定义**：读到另一个事务**未提交**的数据。

### 3.2 不可重复读（Non-Repeatable Read）

```
事务 A：SELECT balance FROM account WHERE id = 1  -- 读到 100
事务 B：UPDATE account SET balance = 200 WHERE id = 1; COMMIT;
事务 A：SELECT balance FROM account WHERE id = 1  -- 读到 200（同一事务两次读不一样）
```

**定义**：同一事务内，两次读同一行数据，**值被修改**了。

### 3.3 幻读（Phantom Read）

```
事务 A：SELECT * FROM account WHERE id BETWEEN 1 AND 10  -- 返回 5 行
事务 B：INSERT INTO account (id, balance) VALUES (5, 100); COMMIT;
事务 A：SELECT * FROM account WHERE id BETWEEN 1 AND 10  -- 返回 6 行（多了一行！）
```

**定义**：同一事务内，两次读同一个范围，**多了或少了行**。

---

## 四、InnoDB RR 如何解决幻读

### 4.1 快照读 vs 当前读

| | 快照读（Snapshot Read） | 当前读（Current Read） |
|---|---|---|
| **SQL** | 普通 `SELECT` | `SELECT ... FOR UPDATE` / `UPDATE` / `DELETE` / `INSERT` |
| **幻读解决** | **MVCC** ReadView | **Next-Key Lock**（行锁+间隙锁） |
| **读到什么** | 事务开始时的快照版本 | 最新已提交版本 + 锁住间隙 |

### 4.2 InnoDB 的 RR 幻读还存在的极端场景

```sql
-- 事务 A
BEGIN;
SELECT * FROM t WHERE id = 5;        -- 快照读，id=5 不存在
UPDATE t SET c = 1 WHERE id = 5;      -- 当前读！因为 UPDATE 是当前读
-- 如果事务 B 刚好插入了 id=5 → 事务 A 的 UPDATE 会更新到！
-- 这算是一种"幻读"的体现（快照读和当前读不一致）
```

> InnoDB 的 RR 在绝大多数场景下解决了幻读，但不是 100% 消除。

---

## 五、隔离级别实现原理对比

| 级别 | ReadView 策略 | 锁策略 |
|---|---|---|
| **READ UNCOMMITTED** | 不使用 MVCC | 读不加锁 |
| **READ COMMITTED** | **每次 SELECT 生成新 ReadView** | 普通读 MVCC，当前读加行锁 |
| **REPEATABLE READ** | **事务开始生成一个 ReadView** | 普通读 MVCC，当前读加 Next-Key Lock |
| **SERIALIZABLE** | 不使用 MVCC | 所有读强制加共享锁 |

---

## 六、核心对比速查表

### 脏读 vs 不可重复读 vs 幻读

| | 脏读 | 不可重复读 | 幻读 |
|---|---|---|---|
| **差异来源** | 未提交数据 | 已提交的 UPDATE/DELETE | 已提交的 INSERT |
| **读到的差异** | 读到了会回滚的数据 | 同一行值变了 | 行数变了 |
| **RC 能解决？** | ✅ | ❌ | ❌ |
| **RR 能解决？** | ✅ | ✅ | ✅\* |

---

## 七、终极背诵总结

1. **ACID 实现**：原子性=undo log，隔离性=锁+MVCC，持久性=redo log
2. **4 种隔离级别**：RU→RC→RR→SERIALIZABLE，InnoDB 默认 RR，Oracle 默认 RC
3. **3 并发问题**：脏读（未提交）→ 不可重复读（值变了）→ 幻读（行数变了）
4. **RR 如何防幻读**：快照读靠 MVCC（ReadView），当前读靠 Next-Key Lock
5. **RC vs RR 核心差异**：RC 每次读生成新 ReadView，RR 事务开始生成一个持续使用
