package com.yc.g_seven_week.W7D1;

public class W7D1_Actual {

    /**
     * 代码实战 ii：Redis 持久化配置与 crash 恢复验证
     *
     * 1. 配置 RDB + AOF 混合持久化（redis.conf）
     *   # ------------------ RDB 配置 ------------------
     *   # 触发 RDB 快照的条件（save 秒数 变化次数）
     *    save 900 1      # 900秒内有1次修改
     *    save 300 10     # 300秒内有10次修改
     *    save 60 10000   # 60秒内有10000次修改
     *    dbfilename dump.rdb
     *    dir ./
     *
     * # ------------------ AOF 配置 ------------------
     *      appendonly yes                  # 开启AOF
     *      appendfilename "appendonly.aof"
     *      appendfsync everysec            # 每秒刷盘一次（平衡性能与安全）
     *      no-appendfsync-on-rewrite no    # 重写时也刷盘
     *      auto-aof-rewrite-percentage 100 # 当AOF文件大小增长100%时触发重写
     *      auto-aof-rewrite-min-size 64mb # 重写最小文件大小
     *      aof-use-rdb-preamble yes        # 开启混合持久化（Redis 4.0+）
     *
     * 2. crash 恢复验证步骤
     *  1.启动 Redis，写入测试数据：
     *     SET name "test"
     *      ZADD game:ranking 100 "user1"
     *  2.直接 kill -9 杀掉 Redis 进程（模拟 crash）
     *  3.重新启动 Redis
     *  4.连接 Redis，查询数据：
     *      GET name
     *      ZRANGE game:ranking 0 -1 WITHSCORES
     *
     * --数据正常读取，说明持久化生效。
     *
     *
     *  画出 RDB 和 AOF 持久化流程对比图
     *
     * 1. RDB 流程（ASCII 版）
     *   触发条件（save/bgsave/配置规则）
     *     ↓
     *   fork() 子进程
     *     ↓
     *   子进程遍历内存数据，写入临时 .rdb 文件
     *     ↓
     *   写入完成后替换旧的 dump.rdb
     *     ↓
     *   主进程继续处理请求
     *
     * 2. AOF 流程（ASCII 版）
     *   客户端写入命令 → 追加到 AOF 缓冲区
     *     ↓
     *   根据 appendfsync 策略刷盘（always/everysec/no）
     *     ↓
     *   AOF 文件增长到阈值 → 触发 AOF 重写
     *     ↓
     *   fork() 子进程，根据当前内存数据生成新的 AOF 文件
     *     ↓
     *   重写完成后替换旧的 AOF 文件
     *
     *  3. RDB vs AOF 对比
     *
     *      维度	                    RDB	                AOF
     *      数据安全性	可能丢失最后一次快照后的数据	    更安全（everysec 最多丢 1 秒数据）
     *      性能影响	    fork 子进程，大内存时可能阻塞	每次写操作追加，性能开销小
     *      文件大小	    压缩后的二进制文件，体积小	    日志 格式，体积大（可重写压缩）
     *      恢复速度	    快（直接加载二进制文件）	    慢（逐条回放命令）
     *
     */
}
