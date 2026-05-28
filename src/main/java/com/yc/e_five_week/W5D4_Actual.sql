-- =====================================================
-- 准备工作：创建测试表与数据
-- =====================================================
DROP TABLE IF EXISTS account;
CREATE TABLE account (
                         id INT PRIMARY KEY AUTO_INCREMENT,
                         name VARCHAR(20),
                         balance DECIMAL(10,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO account(name, balance) VALUES ('张三', 1000.00);

-- =====================================================
-- 前置说明：MySQL 事务隔离级别
-- 1. READ UNCOMMITTED（读未提交）：最低级别，存在脏读、不可重复读、幻读
-- 2. READ COMMITTED（读已提交）：解决脏读，仍存在不可重复读、幻读
-- 3. REPEATABLE READ（可重复读，InnoDB 默认）：解决脏读、不可重复读，存在幻读（部分解决）
-- 4. SERIALIZABLE（串行化）：解决所有并发问题，性能最差
-- =====================================================

-- =====================================================
-- 场景1：脏读演示（需设置隔离级别为 READ UNCOMMITTED）
-- 现象：事务A能读到事务B未提交的修改，若B回滚，A读到的数据无效
-- =====================================================

-- 【事务A】（终端1）
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED; -- 设置隔离级别
START TRANSACTION;
SELECT balance FROM account WHERE name = '张三'; -- 读到 1000.00，后续会读到脏数据
-- 不提交，保持事务打开

-- 【事务B】（终端2）
START TRANSACTION;
UPDATE account SET balance = 2000.00 WHERE name = '张三'; -- 修改但不提交
-- 此时回到事务A，再次查询：
-- SELECT balance FROM account WHERE name = '张三'; -- 读到 2000.00（脏读）
ROLLBACK; -- 事务B回滚，数据变回1000.00

-- 【事务A】再次查询：
-- SELECT balance FROM account WHERE name = '张三'; -- 数据变回1000.00，之前读到的是脏数据
COMMIT; -- 事务A提交

-- =====================================================
-- 场景2：不可重复读演示（需设置隔离级别为 READ COMMITTED）
-- 现象：同一事务内，两次读取同一数据，结果不一致（被其他事务修改并提交）
-- =====================================================

-- 【事务A】（终端1）
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED; -- 设置隔离级别
START TRANSACTION;
SELECT balance FROM account WHERE name = '张三'; -- 第一次读到 1000.00
-- 不提交，保持事务打开

-- 【事务B】（终端2）
START TRANSACTION;
UPDATE account SET balance = 3000.00 WHERE name = '张三'; -- 修改并提交
COMMIT;

-- 【事务A】再次查询：
-- SELECT balance FROM account WHERE name = '张三'; -- 第二次读到 3000.00，两次结果不一致（不可重复读）
COMMIT; -- 事务A提交

-- =====================================================
-- 场景3：幻读演示（需设置隔离级别为 REPEATABLE READ）
-- 现象：同一事务内，两次查询同一范围的数据，行数不一致（被其他事务插入新数据）
-- 注：InnoDB 的 RR 级别通过 Next-Key Lock 部分解决幻读，需特殊场景触发
-- =====================================================

-- 【事务A】（终端1）
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ; -- 设置默认隔离级别
START TRANSACTION;
SELECT * FROM account WHERE balance > 500; -- 第一次查到1条数据（张三 1000）
-- 不提交，保持事务打开

-- 【事务B】（终端2）
START TRANSACTION;
INSERT INTO account(name, balance) VALUES ('李四', 1500.00); -- 插入新数据并提交
COMMIT;

-- 【事务A】再次查询：
-- SELECT * FROM account WHERE balance > 500; -- 第二次仍只查到1条（MVCC快照读）
-- 但如果在事务A中执行更新操作：
-- UPDATE account SET balance = 4000 WHERE balance > 500; -- 会同时更新张三和李四的数据（当前读）
-- 再次查询：
-- SELECT * FROM account WHERE balance > 500; -- 此时查到2条，出现“幻读”现象
COMMIT; -- 事务A提交

-- 清理测试数据
DELETE FROM account WHERE name = '李四';

-- =====================================================
-- 补充：查看当前会话的隔离级别
-- =====================================================
SELECT @@tx_isolation; -- 旧版MySQL
-- 或
SELECT @@transaction_isolation; -- 新版MySQL 8.0+

-- =====================================================
-- 【作业1】隔离级别 vs 并发问题对照表
-- =====================================================
-- | 隔离级别                     | 脏读       |   不可重复读   |          幻读 |
-- |-----------------------     |------     |------------  |------          |
-- | READ UNCOMMITTED           | ❌ 不能解决 | ❌ 不能解决  | ❌ 不能解决 |
-- | READ COMMITTED             | ✅ 解决    | ❌ 不能解决  | ❌ 不能解决 |
-- | REPEATABLE READ(InnoDB默认) | ✅ 解决    | ✅ 解决     | ⚠️ 部分解决（Next-Key Lock） |
-- | SERIALIZABLE               | ✅ 解决    | ✅ 解决     | ✅ 解决 |

-- =====================================================
-- 【作业2】ACID 各由什么机制保证（口述版）
-- =====================================================
-- A（Atomicity 原子性）：
-- 由 Undo Log（回滚日志）保证。事务要么全部成功提交，要么全部失败回滚，失败时通过Undo Log恢复到执行前状态。
--
-- C（Consistency 一致性）：
-- 由业务规则+其他三性共同保证。数据库从一个一致状态变为另一个一致状态，事务执行前后数据完整性约束不被破坏。
--
-- I（Isolation 隔离性）：
-- 由 事务隔离级别 + MVCC（多版本并发控制） + 锁机制（行锁/间隙锁/临键锁）保证。隔离级别定义事务之间的可见性，MVCC和锁解决并发冲突。
--
-- D（Durability 持久性）：
-- 由 Redo Log（重做日志）保证。事务提交后数据永久保存，即使系统崩溃，也能通过Redo Log恢复数据。