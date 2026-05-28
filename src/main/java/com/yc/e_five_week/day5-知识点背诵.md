# Day5 MVCC、undo log、redo log、binlog 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、MVCC（多版本并发控制）核心

### 1.1 什么是 MVCC

**多版本并发控制**：通过维护数据的**多个历史版本**，实现**读不阻塞写、写不阻塞读**。

```
读操作 → 读的是事务开始时的快照（历史版本）
写操作 → 创建新版本（旧版本保留给正在读的事务使用）
```

**一句话**：读写互不阻塞，靠的是"读旧版本 + 写新版本"。

### 1.2 两隐藏列 + 版本链

InnoDB 每行记录有两个隐藏列：

| 隐藏列 | 作用 | 大小 |
|---|---|---|
| **trx_id** | 最近修改此行的事务 ID | 6 字节 |
| **roll_pointer** | 回滚指针，指向上一个版本（undo log 中的旧记录） | 7 字节 |

```
版本链（同一行数据的多个版本用 roll_pointer 串起来）：

当前最新版本（trx_id=105）
  │ roll_pointer
  ▼
历史版本1（trx_id=100）
  │ roll_pointer
  ▼
历史版本2（trx_id=90）
  │ roll_pointer
  ▼
  NULL
```

### 1.3 ReadView（可见性判断核心）

**ReadView 包含**：
- `m_ids`：当前活跃事务 ID 列表（未提交的事务）
- `min_trx_id`：活跃事务中最小的事务 ID
- `max_trx_id`：下一个将要分配的事务 ID
- `creator_trx_id`：创建这个 ReadView 的事务 ID

**可见性判断规则**（判断某个记录的 trx_id 是否对当前事务可见）：

```
被访问版本的 trx_id = creator_trx_id → 可见（自己的修改）
被访问版本的 trx_id < min_trx_id      → 可见（修改已经提交）
被访问版本的 trx_id ≥ max_trx_id      → 不可见（将来的事务改的）
min_trx_id ≤ trx_id < max_trx_id      → 在活跃列表中？不可见 : 可见
```

### 1.4 RC vs RR 下 ReadView 的差异（面试重点）

| | READ COMMITTED | REPEATABLE READ |
|---|---|---|
| **ReadView 生成时机** | **每次 SELECT** 都重新生成 | **事务开始时**生成，之后不变 |
| **可见性效果** | 每次读看到最新已提交数据 | 整个事务看到相同快照 |
| **不可重复读** | 有 | **无**（两次读用同一个 ReadView） |

> RC = 每次都看最新快照 → 同事务内两次读可能不同 → 有不可重复读
> RR = 一次看定 → 同事务内读一样 → 解决不可重复读

---

## 二、undo log（回滚日志）

### 2.1 核心作用

| 作用 | 说明 |
|---|---|
| **事务回滚** | `ROLLBACK` 时根据 undo log 恢复到修改前的值 |
| **MVCC 读历史版本** | 快照读通过 roll_pointer 沿 undo log 链找到可见版本 |
| **崩溃恢复辅助** | redo log 重放后，undo log 回滚未提交的事务 |

### 2.2 undo log 类型

| 类型 | 记录内容 | 对应操作 |
|---|---|---|
| **INSERT undo log** | 插入记录的主键 | `INSERT` |
| **UPDATE undo log** | 修改前列值 | `UPDATE` / `DELETE` |

- INSERT undo log：事务提交后**立即删除**（不需要给 MVCC 保留历史）
- UPDATE undo log：需要保留到**没有事务需要用到这个旧版本**（purge 线程清理）

### 2.3 undo log 的存储

- 存储在 undo 表空间（`innodb_undo_tablespaces`）
- 默认 128 个回滚段（rollback segment），每个段 1024 个槽
- undo log 也采用页管理（16KB）

---

## 三、redo log（重做日志）⭐⭐⭐

### 3.1 核心定义

**物理日志**，记录"对哪个数据页的什么位置做了什么修改"。

```
redo log 记录格式：
  space_id + page_no + offset + 修改内容

例如：在表空间 0、页号 12、偏移量 200 处写入 4 字节值 100
```

### 3.2 WAL（Write Ahead Log）策略

```
更新一条记录的完整流程：

① 修改 Buffer Pool 中的数据页（脏页，未刷盘）
② 记录 redo log（先写 Log Buffer → 事务提交时刷到磁盘）
③ 事务提交成功（redo log 已持久化 → 就算数据页没刷盘，崩溃也能恢复）
④ 后台线程将脏页刷回磁盘（checkpoint）
```

**核心**：先写日志再写数据（WAL），保证持久性的同时最大化性能。

### 3.3 redo log 循环写

```
redo log 是固定大小的循环文件：

┌───┐ → 写 → ┌───┐ → 写 → ┌───┐ → 写 → ┌───┐
│ 1 │        │ 2 │        │ 3 │        │ 4 │
└───┘        └───┘        └───┘        └───┘
  ↑                                      │
  └──────────────────────────────────────┘
              循环覆盖

write pos：当前写入位置
checkpoint：当前已刷盘位置
两者之间是可写空间
```

**关键参数**：
- `innodb_log_file_size`：每个 redo log 文件大小（默认 48MB）
- `innodb_log_files_in_group`：文件数量（默认 2）
- `innodb_flush_log_at_trx_commit`：刷盘策略

### 3.4 flush_log_at_trx_commit 三种策略

| 值 | 策略 | 持久性 | 性能 |
|---|---|---|---|
| **0** | 每秒刷一次 Log Buffer → OS buffer → 磁盘 | 可能丢 1 秒数据 | 最高 |
| **1**（默认） | 每次提交都刷到磁盘 | **不会丢数据** | 中等 |
| **2** | 每次提交刷到 OS buffer，每秒刷磁盘 | MySQL 崩溃不丢，OS 崩溃丢 | 较高 |

> 金融/支付场景用 1，日志/监控等可牺牲一点持久性用 2。

---

## 四、binlog（归档日志）

### 4.1 binlog vs redo log

| | redo log | binlog |
|---|---|---|
| **层级** | **InnoDB 引擎层** | **MySQL Server 层** |
| **日志类型** | **物理日志**（修改了哪个页的哪个位置） | **逻辑日志**（SQL 语句/行变更） |
| **写入方式** | **循环写**（不保留历史） | **追加写**（保留历史） |
| **用途** | 崩溃恢复（crash recovery） | 主从复制 + 基于时间点的数据恢复 |
| **刷盘控制** | `innodb_flush_log_at_trx_commit` | `sync_binlog` |

### 4.2 binlog 三种格式

| 格式 | 记录内容 | 优点 | 缺点 |
|---|---|---|---|
| **STATEMENT** | SQL 语句文本 | 日志小 | 部分函数（NOW/UUID）主从不一致 |
| **ROW**（默认） | 每行变更前后值 | 精确，不会不一致 | 日志大 |
| **MIXED** | STATEMENT + ROW 自动切换 | 折中 | 复杂 |

### 4.3 两阶段提交

```
为什么需要两阶段提交？
  问题：redo log 和 binlog 可能不一致
  场景：redo log 写成功但 binlog 失败 → 主库恢复了但从库没有这条记录

两阶段提交流程：
  ┌─────────────────────┐
  │ ① redo log prepare  │ ← 写 redo log，标记 prepare 状态
  └────────┬────────────┘
           ▼
  ┌─────────────────────┐
  │ ② write binlog      │ ← 写 binlog
  └────────┬────────────┘
           ▼
  ┌─────────────────────┐
  │ ③ redo log commit   │ ← redo log 改为 commit 状态
  └─────────────────────┘

崩溃恢复判断：
  - binlog 完整 + redo log prepare → 提交（补 commit）
  - binlog 不完整 → 回滚
```

---

## 五、终极背诵总结

1. **MVCC 三要素**：trx_id（谁改的）+ roll_pointer（版本链）+ ReadView（谁看得见）
2. **ReadView 可见性规则**：小于 min 可见，大于 max 不可见，中间看活跃列表
3. **RC vs RR**：RC 每次读新 ReadView，RR 事务开始订一个用到底
4. **undo log**：逻辑日志，记修改前的值，用于回滚 + MVCC
5. **redo log**：物理日志，记修改了什么，WAL 先写日志，循环写，用于崩溃恢复
6. **binlog**：逻辑日志，记 SQL/行变更，追加写，用于主从复制
7. **两阶段提交**：redo prepare → binlog → redo commit，保证 redo 和 binlog 一致
