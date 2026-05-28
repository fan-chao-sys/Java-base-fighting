-- ======================================================
-- 实验目标：验证 二级索引、回表、覆盖索引
-- 核心知识点：
-- 1. 普通二级索引：查询非索引字段 → 发生【回表】，Extra 显示 Using index condition
-- 2. 覆盖索引：查询字段全部包含在索引中 → 避免回表，Extra 显示 Using index
-- ======================================================

-- 1. 创建测试表
DROP TABLE IF EXISTS user_info;
CREATE TABLE user_info (
                           id INT PRIMARY KEY AUTO_INCREMENT,  -- 主键（聚簇索引）
                           username VARCHAR(32) NOT NULL,
                           age INT,
                           email VARCHAR(64)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '索引测试表';

-- 2. 批量插入测试数据
INSERT INTO user_info(username,age,email)
VALUES
    ('zhangsan',20,'zs@test.com'),
    ('lisi',22,'ls@test.com'),
    ('wangwu',25,'ww@test.com'),
    ('zhaoliu',28,'zl@test.com'),
    ('sunqi',30,'sq@test.com');

-- 3. 创建【普通二级单列索引】(username)
-- 索引结构：二级索引存储 username + 主键id
CREATE INDEX idx_username ON user_info(username);

-- ======================================================
-- 场景一：普通查询 → 触发 回表
-- 查询字段包含 索引列 + 非索引列(age/email)
-- Extra 输出：Using index condition  代表需要回表查询聚簇索引拿数据
-- ======================================================
EXPLAIN
SELECT username, age FROM user_info WHERE username = 'lisi';

-- ======================================================
-- 场景二：仅查询索引列 → 覆盖索引，避免回表
-- 查询字段只有索引列 username，索引中已包含全部所需数据
-- Extra 输出：Using index  代表使用覆盖索引，无回表
-- ======================================================
EXPLAIN
SELECT username FROM user_info WHERE username = 'lisi';

-- ======================================================
-- 场景三：创建【联合覆盖索引】(username,age)
-- 索引同时包含查询条件 + 查询字段，实现覆盖索引
-- ======================================================
DROP INDEX idx_username ON user_info; -- 删除原有单列索引
CREATE INDEX idx_username_age ON user_info(username, age);

-- 再次查询 username + age，全部字段在联合索引内
-- Extra 依然显示 Using index，无回表
EXPLAIN
SELECT username, age FROM user_info WHERE username = 'wangwu';

-- 再次查询包含 email（非索引字段），触发回表
EXPLAIN
SELECT username, age, email FROM user_info WHERE username = 'wangwu';

-- ======================================================
-- 收尾：清理测试环境（可选执行）
-- ======================================================
-- DROP TABLE IF EXISTS user_info;