-- =====================================================
-- 准备工作：创建测试表
-- =====================================================
DROP TABLE IF EXISTS mvcc_test;
CREATE TABLE mvcc_test (
                           id INT PRIMARY KEY AUTO_INCREMENT,
                           name VARCHAR(20),
                           balance DECIMAL(10,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO mvcc_test(name, balance) VALUES ('test', 1000.00);

-- =====================================================
-- 【代码实战 i】用 SHOW ENGINE INNODB STATUS 和 SHOW MASTER STATUS
-- 观察 redo log 和 binlog 写入情况
-- =====================================================

-- 1. 事务开始前，查看初始状态
-- 查看 redo log 状态（重点看 LOG 部分）
SHOW ENGINE INNODB STATUS;

-- 查看 binlog 状态（当前 binlog 文件和位置）
SHOW MASTER STATUS;

-- 2. 开启事务并执行更新操作（触发 redo log 和 binlog 写入）
START TRANSACTION;
UPDATE mvcc_test SET balance = balance + 100 WHERE id = 1; -- 第1次更新
UPDATE mvcc_test SET balance = balance + 200 WHERE id = 1; -- 第2次更新

-- 3. 事务提交前，再次查看状态
-- redo log 已经写入 prepare 阶段
SHOW ENGINE INNODB STATUS;
-- binlog 尚未写入（或正在写入）
SHOW MASTER STATUS;

-- 4. 提交事务（触发两阶段提交）
COMMIT;

-- 5. 提交后，再次查看状态
-- redo log 完成 commit，binlog 位置更新
SHOW ENGINE INNODB STATUS;
SHOW MASTER STATUS;

-- 关键观察点：
-- 1. SHOW ENGINE INNODB STATUS 中的 LOG 部分，会显示 redo log 的 LSN（日志序列号）增长
-- 2. SHOW MASTER STATUS 中的 Position 字段，binlog 文件位置会随着事务提交而增加

-- =====================================================
-- 【代码实战 ii】事务中多次更新同一行，分析 MVCC 版本链
-- 使用 SELECT ... FOR SYSTEM_TIME 查看历史版本（MySQL 8.0+ 支持）
-- =====================================================

-- 1. 开启事务，多次更新同一行
START TRANSACTION;
UPDATE mvcc_test SET balance = 1200 WHERE id = 1; -- 版本1
UPDATE mvcc_test SET balance = 1500 WHERE id = 1; -- 版本2
UPDATE mvcc_test SET balance = 2000 WHERE id = 1; -- 版本3

-- 2. 查看当前版本
SELECT * FROM mvcc_test WHERE id = 1;

-- 3. 查看该行的所有历史版本（需开启系统版本控制，MySQL 8.0+）
-- 先为表开启系统版本控制
ALTER TABLE mvcc_test ADD COLUMN `start` TIMESTAMP(6) GENERATED ALWAYS AS ROW START,
    ADD COLUMN `end` TIMESTAMP(6) GENERATED ALWAYS AS ROW END,
    ADD PERIOD FOR SYSTEM_TIME (`start`, `end`);
ALTER TABLE mvcc_test WITH SYSTEM VERSIONING;

-- 查看该行的所有版本链
SELECT * FROM mvcc_test FOR SYSTEM_TIME ALL WHERE id = 1;

-- 4. 提交事务
COMMIT;

-- 手动分析版本链（核心逻辑）：
-- 每次更新都会生成一个新版本，旧版本保留在 undo log 中
-- 版本链结构：最新版本 ← undo log ← 旧版本 ← undo log ← 更旧版本...
-- 每个版本包含：trx_id（事务ID）、roll_pointer（回滚指针）

-- =====================================================
-- 【今日作业1】MVCC 版本链结构图（文本版）
-- =====================================================
/*
        当前行数据（聚簇索引）
        ┌─────────────────────────────────┐
        | id=1, balance=2000, trx_id=100, |
        | roll_pointer ↓                  |
        └─────────────────────────────────┘
                  │
                  ▼
        ┌─────────────────────────────────┐
        | balance=1500, trx_id=99,        |
        | roll_pointer ↓                  |
        └─────────────────────────────────┘
                  │
                  ▼
        ┌─────────────────────────────────┐
        | balance=1200, trx_id=98,       |
        | roll_pointer ↓                  |
        └─────────────────────────────────┘
                  │
                  ▼
        ┌─────────────────────────────────┐
        | balance=1000, trx_id=97,        |
        | roll_pointer = NULL             |
        └─────────────────────────────────┘

        ReadView 可见性判断逻辑：
        1. trx_id < min_trx_id → 可见
        2. trx_id 在 [min_trx_id, max_trx_id] 且不在活跃事务列表 → 可见
        3. trx_id > max_trx_id → 不可见
*/

-- =====================================================
-- 【今日作业2】两阶段提交流程图（文本版）
-- =====================================================
/*
        事务执行阶段
              │
              ▼
        ┌─────────────────┐
        | 1. Redo Log Prepare |
        └─────────────────┘
              │
              ▼
        ┌─────────────────┐
        | 2. 写入 Binlog    |
        └─────────────────┘
              │
              ▼
        ┌─────────────────┐
        | 3. Redo Log Commit |
        └─────────────────┘

        崩溃恢复逻辑：
        - 如果崩溃发生在 1 和 2 之间：Redo Log 有 Prepare 标记，Binlog 无数据 → 事务回滚
        - 如果崩溃发生在 2 和 3 之间：Redo Log 有 Prepare 标记，Binlog 有完整数据 → 事务提交
*/