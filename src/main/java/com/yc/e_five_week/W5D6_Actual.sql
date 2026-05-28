-- =====================================================
-- 准备工作：创建测试表与数据
-- =====================================================
DROP TABLE IF EXISTS user_account;
CREATE TABLE user_account (
                              id INT PRIMARY KEY AUTO_INCREMENT,
                              name VARCHAR(20),
                              balance DECIMAL(10,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO user_account(name, balance) VALUES
                                            ('A', 1000.00),
                                            ('B', 1000.00),
                                            ('C', 1000.00),
                                            ('D', 1000.00),
                                            ('E', 1000.00);

-- 为业务SQL创建索引
CREATE INDEX idx_name ON user_account(name);
CREATE INDEX idx_balance ON user_account(balance);
CREATE INDEX idx_name_balance ON user_account(name, balance);

-- =====================================================
-- 【代码实战 i】死锁 Demo（两个事务互相等待对方的锁）
-- 用 SHOW ENGINE INNODB STATUS 查看死锁信息
-- =====================================================

-- 【事务A】（终端1）
START TRANSACTION;
-- 第一步：锁住A的账户
UPDATE user_account SET balance = balance - 100 WHERE name = 'A';
-- 暂停，不要提交

-- 【事务B】（终端2）
START TRANSACTION;
-- 第一步：锁住B的账户
UPDATE user_account SET balance = balance - 100 WHERE name = 'B';
-- 暂停，不要提交

-- 【事务A】继续执行：请求B的账户锁（被事务B持有）
UPDATE user_account SET balance = balance + 100 WHERE name = 'B';

-- 【事务B】继续执行：请求A的账户锁（被事务A持有）
-- 此时触发死锁，InnoDB会自动回滚其中一个事务，抛出死锁错误
UPDATE user_account SET balance = balance + 100 WHERE name = 'A';

-- 查看死锁信息（关键命令）
SHOW ENGINE INNODB STATUS;
-- 重点关注 LATEST DETECTED DEADLOCK 部分，包含：
-- 1. 两个事务的执行语句
-- 2. 各自持有的锁、等待的锁
-- 3. 回滚了哪个事务

-- 清理事务
ROLLBACK; -- 事务A/B都需要回滚或提交

-- =====================================================
-- 【代码实战 ii】5条业务SQL的EXPLAIN分析
-- 指出 type、key、Extra 含义并评估优劣
-- =====================================================

-- 业务SQL1：主键等值查询（最优）
EXPLAIN
SELECT * FROM user_account WHERE id = 1;
-- 分析：
-- type: const → 主键/唯一索引等值匹配，单条记录，性能最高
-- key: PRIMARY → 使用主键索引
-- Extra: NULL → 无额外操作，性能最优

-- 业务SQL2：二级索引等值查询（回表）
EXPLAIN
SELECT * FROM user_account WHERE name = 'A';
-- 分析：
-- type: ref → 非唯一索引等值匹配
-- key: idx_name → 使用name列的二级索引
-- Extra: Using index condition → 回表查询聚簇索引，存在额外IO

-- 业务SQL3：覆盖索引查询（无回表）
EXPLAIN
SELECT name, balance FROM user_account WHERE name = 'A';
-- 分析：
-- type: ref → 等值匹配
-- key: idx_name_balance → 使用联合索引
-- Extra: Using index → 覆盖索引，无需回表，性能优于SQL2

-- 业务SQL4：范围查询（部分索引生效）
EXPLAIN
SELECT * FROM user_account WHERE name > 'A' AND balance > 500;
-- 分析：
-- type: range → 索引范围扫描
-- key: idx_name_balance → 使用联合索引的前缀
-- Extra: Using index condition → 范围查询截断后续索引，需回表过滤balance，性能一般

-- 业务SQL5：全表扫描（性能最差）
EXPLAIN
SELECT * FROM user_account WHERE balance = 1000;
-- 分析：
-- type: ALL → 全表扫描，未使用索引
-- key: NULL → 无索引使用
-- Extra: Using where → 服务器层过滤数据，性能最差

-- =====================================================
-- 【今日作业1】死锁排查流程笔记
-- =====================================================
/*
死锁排查流程（面试必背）：
1.  触发死锁后，执行 SHOW ENGINE INNODB STATUS，定位 LATEST DETECTED DEADLOCK 部分
2.  查看两个事务的执行SQL，确认它们的加锁顺序和锁类型
3.  分析死锁成因：
    - 加锁顺序不一致（最常见）
    - 持有锁后长时间未提交
    - 索引失效导致行锁升级为表锁
4.  解决方法：
    - 统一事务中加锁顺序
    - 优化SQL，确保索引生效，避免行锁升级
    - 缩短事务执行时间，减少锁持有时间
    - 降低隔离级别（如RR→RC，减少间隙锁）
*/

-- =====================================================
-- 【今日作业2】EXPLAIN type字段各值含义及性能排序（口述版）
-- =====================================================
/*
性能从优到劣排序：
const > eq_ref > ref > range > index > ALL

各值含义：
1. const：主键/唯一索引等值匹配，仅返回1条数据，性能最优
2. eq_ref：唯一索引关联查询（如JOIN主键），每条记录匹配1条数据
3. ref：非唯一索引等值匹配，可能返回多条数据
4. range：索引范围扫描（> < BETWEEN），扫描部分索引数据
5. index：全索引扫描，遍历整个索引树，比全表扫描略快
6. ALL：全表扫描，无索引使用，性能最差
*/

-- 清理测试数据
DROP TABLE IF EXISTS user_account;