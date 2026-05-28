-- =============================================
-- 测试：MyISAM 与 InnoDB 引擎 SELECT COUNT(*) 性能对比
-- 核心结论：
-- MyISAM：表自带总行数缓存，COUNT(*) 瞬间返回（O(1)）
-- InnoDB：无行数缓存，需要扫描索引统计行数（O(n)）
-- =============================================

-- 1. 创建 MyISAM 引擎表（自带总行数缓存）
CREATE TABLE IF NOT EXISTS test_myisam (
                                           id INT PRIMARY KEY AUTO_INCREMENT  -- 主键自增
) ENGINE = MyISAM COMMENT 'MyISAM引擎表，用于COUNT(*)性能测试';

-- 2. 创建 InnoDB 引擎表（支持事务，无行数缓存）
CREATE TABLE IF NOT EXISTS test_innodb (
                                           id INT PRIMARY KEY AUTO_INCREMENT  -- 主键自增
) ENGINE = InnoDB COMMENT 'InnoDB引擎表，用于COUNT(*)性能测试';

-- =============================================
-- 批量插入测试数据（插入1万条，用于测试性能差距）
-- 说明：数据越多，InnoDB COUNT(*) 越慢，MyISAM 依然秒出
-- =============================================
INSERT INTO test_myisam VALUES (),(),(),(),(),(),(),(),(),(); -- 10条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 20条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 40条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 80条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 160条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 320条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 640条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 1280条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 2560条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 5120条
INSERT INTO test_myisam SELECT NULL FROM test_myisam; -- 10240条

-- 给 InnoDB 表插入同样数量的数据
INSERT INTO test_innodb SELECT NULL FROM test_myisam;

-- =============================================
-- 执行 COUNT(*) 性能测试
-- =============================================

-- 测试1：MyISAM COUNT(*)
-- 效果：瞬间返回
-- 原因：MyISAM 引擎在表元数据中存储了总行数，直接读取，无需遍历表
SELECT COUNT(*) AS myisam_count FROM test_myisam;

-- 测试2：InnoDB COUNT(*)
-- 效果：需要扫描索引，数据量越大越慢
-- 原因：InnoDB 支持事务 + MVCC，每行可见性不确定，无法缓存总行数，必须实时统计
SELECT COUNT(*) AS innodb_count FROM test_innodb;

-- =============================================
-- 查看两张表的存储引擎（验证）
-- =============================================
SHOW TABLE STATUS LIKE 'test_myisam';
SHOW TABLE STATUS LIKE 'test_innodb';

-- =============================================
-- 清理测试表（测试完成后执行）
-- =============================================
-- DROP TABLE IF EXISTS test_myisam;
-- DROP TABLE IF EXISTS test_innodb;





-- =============================================
-- 测试：用 SHOW ENGINE INNODB STATUS 查看 InnoDB 状态
-- =============================================
-- =============================================
-- InnoDB 状态查看实验
-- 命令：SHOW ENGINE INNODB STATUS;
-- 作用：查看 InnoDB 引擎运行时状态、事务、锁、日志、缓冲池等
-- =============================================

-- 1. 创建测试表（InnoDB）
CREATE TABLE IF NOT EXISTS innodb_test (
                                           id INT PRIMARY KEY AUTO_INCREMENT,
                                           name VARCHAR(20)
) ENGINE = InnoDB;

-- 2. 插入测试数据
INSERT INTO innodb_test(name) VALUES ('test1'), ('test2'), ('test3');

-- =============================================
-- 3. 核心命令：查看 InnoDB 完整状态
-- =============================================
SHOW ENGINE INNODB STATUS;

-- =============================================
-- 【重点】SHOW ENGINE INNODB STATUS 输出结构说明（面试/学习必背）
-- =============================================
-- 1. BACKGROUND THREAD
--    说明：InnoDB 后台线程（IO 线程、Purge 线程、Master 线程）

-- 2. SEMAPHORES
--    说明：信号量、线程等待、锁等待情况

-- 3. TRANSACTIONS
--    说明：【最重要】当前运行的事务、锁等待、未提交事务、MVCC 信息
--    常见用途：查看死锁、查看长事务

-- 4. FILE I/O
--    说明：IO 线程状态、IO 请求次数、读写性能

-- 5. INSERT BUFFER AND ADAPTIVE HASH INDEX
--    说明：插入缓冲、自适应哈希索引状态

-- 6. LOG
--    说明：Redo Log 状态、日志序列号、刷新情况

-- 7. BUFFER POOL AND MEMORY
--    说明：缓冲池使用情况、命中率、脏页数量
--    关键：InnoDB 内存性能核心指标

-- 8. ROW OPERATIONS
--    说明：增删改查行数、事务提交次数、性能统计

-- =============================================
-- 4. 查看当前 InnoDB 事务（快速排查锁/事务问题）
-- =============================================
SELECT * FROM information_schema.INNODB_TRX;

-- =============================================
-- 5. 查看 InnoDB 锁等待（排查死锁必备）
-- =============================================
SELECT * FROM information_schema.INNODB_LOCK_WAITS;

-- =============================================
-- 6. 查看当前持有锁（排查锁阻塞）
-- =============================================
SELECT * FROM information_schema.INNODB_LOCKS;

-- =============================================
-- 7. 测试完成后清理（可选）
-- =============================================
-- DROP TABLE IF EXISTS innodb_test;