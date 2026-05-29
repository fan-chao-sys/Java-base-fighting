# Day7 周测知识点总结 —— Redis、MQ、网络、分布式基础

> 覆盖 Day1~Day6 全部核心知识点，对应"学习任务清单" Day 7 整周复盘要求。

---

## 一、10 道自测题（口述标准答案）

### Q1: Redis 为什么快？（至少 3 个维度）

**答：**

| 维度 | 原因 |
|---|---|
| **纯内存操作** | 数据在内存中，读写纳秒级（磁盘是毫秒级） |
| **单线程模型** | 无多线程竞争开销、无上下文切换（6.0+ 网络 IO 多线程，执行仍是单线程） |
| **IO 多路复用** | epoll 机制，一个线程处理成千上万个连接 |
| **高效数据结构** | SDS / 跳跃表 / quicklist / ziplist 都是为 Redis 定制的 |
| **RESP 协议简单** | 文本协议，解析极快 |

---

### Q2: 缓存穿透、击穿、雪崩分别怎么解决？

**答：**

| 问题 | 现象 | 解决方案 |
|---|---|---|
| **穿透** | 查不存在的数据，请求穿过缓存直打 DB | ① **布隆过滤器**前置拦截 ② 缓存 null 值设短过期 |
| **击穿** | 热点 key 过期瞬间，大量请求涌向 DB | ① **互斥锁**（只让一个线程查 DB）② 逻辑永不过期+异步刷新 |
| **雪崩** | 大量 key 同时过期 / Redis 宕机 | ① 过期时间加**随机值** ② 主从+哨兵高可用 ③ 限流降级 |

**本周代码（W7D2/RedisDistributedLock）**：`SET NX PX` 加锁 + Lua 脚本安全解锁 = 互斥锁防击穿。

---

### Q3: Redis 分布式锁有哪些坑？怎么用 Redisson 避坑？

**答（W7D2/RedisDistributedLock 完整实现）：**

| 坑 | 现象 | 解决方案 |
|---|---|---|
| **锁超时释放** | 业务没执行完锁过期了，其他线程误入 | **WatchDog 自动续期**（每 10s 续 30s） |
| **误删锁** | A 的锁被 B 释放 | value 设唯一标识 + **Lua 脚本校验后删除** |
| **单点故障** | Redis 挂了锁全丢 | **RedLock**（N/2+1 个节点加锁成功才算） |
| **主从切换丢锁** | 主节点加的锁，从节点还没同步就挂了 | RedLock 缓解 / ZooKeeper |

**本周代码验证**：
```java
// 加锁：SET key value NX PX timeout
stringRedisTemplate.opsForValue().setIfAbsent(key, requestId, timeout, TimeUnit.MILLISECONDS);

// Lua 解锁：先判断 value 是否为 requestId，是才删除
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
```

---

### Q4: MQ 如何保证消息不丢？从生产端到消费端完整说一遍

**答：**

```
全链路保障三环节：

① 生产者 → 发送确认机制
   ┌── confirm 回调 / 事务消息（RocketMQ）
   └── 发送失败自动重试

② Broker → 持久化 + 同步复制
   ┌── 同步刷盘（刷到磁盘才返回成功）
   └── 主从同步复制（slave 也写入才返回）

③ 消费者 → 手动 ACK
   ┌── 严禁自动 ack（auto commit）
   └── 消费成功再手动提交 offset
```

**RocketMQ 零丢失配置**：同步刷盘 + 同步复制 + 手动 ack。

---

### Q5: 消息重复消费怎么处理？

**答（W7D3_Actual 完整幂等方案验证）：**

```
幂等核心：消息 ID + SETNX + 消费状态标记

消息到达 → 1. 解析 messageId
           → 2. Redis SETNX 锁住 messageId（防并发重复）
           → 3. 执行业务
           → 4. 成功：设置永不过期标记 / 失败：删锁，等重试
           → 5. 返回 ACK
```

**四种幂等方案**：

| 方案 | 做法 |
|---|---|
| **Redis SETNX** | `setIfAbsent(msgId, "1", 30s)` 消费前打标 |
| **数据库唯一索引** | 业务唯一 ID + `UNIQUE INDEX`，insert 去重 |
| **数据库乐观锁** | `UPDATE ... SET version=version+1 WHERE version=#{old}` |
| **消费记录表** | 先查记录，已处理则跳过 |

---

### Q6: TCP 为什么是三次握手？（两次不行吗？）

**答：**

```
Client                              Server
  │──── SYN seq=x ──────────────────→│  ① 我来了
  │←─── SYN+ACK seq=y ack=x+1 ────│  ② 你好，收到了
  │──── ACK ack=y+1 ───────────────→│  ③ 好，连接建立
```

**两次不行**：如果只有 SYN → SYN+ACK，Server 发出 SYN+ACK 后认为连接建立。但 Client 的 SYN 可能是**旧连接超时重发的**——Client 已经不要这个连接了，Server 却傻等着浪费资源。

**三次刚好**：Client 必须最后回 ACK，Server 收到后才建立连接。老的无效 SYN 不会触发第三次 ACK → Server 不会白白等待。

---

### Q7: 为什么断开连接是四次挥手？

**答：** TCP 是**全双工**的—双方都能独立收发。`FIN` 只表示"我不发了"但还能**接收**。

```
Client                     Server
  │── FIN ────────────────→│  ① Client：我不发数据了
  │←── ACK ───────────────│  ② Server：收到
  │                         │     (Server 可能还有数据要发)
  │←── FIN ───────────────│  ③ Server：我也不发了
  │── ACK ────────────────→│  ④ Client：收到
  │ 等 2MSL 后关闭           │
```

**中间 ACK 和 FIN 必须分开发**：Server 收到 FIN 后可能还有数据没发完 → ACK 先确认"知道了" → 数据发完再发 FIN → 所以四次。

**等 2MSL 的原因**：① 确保最后一个 ACK 能到 Server ② 让网络残留包消失，不影响新连接。

---

### Q8: HTTPS 的 TLS 握手流程是怎样的？

**答：** HTTPS = HTTP + TLS/SSL，多三次保障：**加密 + 身份认证 + 防篡改**。

```
简版 TLS 1.2 四次交互：

① Client → Server：ClientHello（支持的加密套件 + 随机数1）
② Server → Client：ServerHello（选定加密套件 + 随机数2）+ 证书
③ Client → Server：验证证书 → 生成 PreMaster（用公钥加密）→ 双方算出会话密钥
④ Client ← Server：加密通信开始（对称加密传数据）
```

**核心**：非对称加密交换密钥 → 对称加密传输数据（快）。

---

### Q9: 接口幂等有哪些实现方案？

**答（W7D6/IdempotentController 完整验证）：**

| 方案 | 做法 | 适用场景 |
|---|---|---|
| **Token 机制** | 先获取 token → 提交时校验+删除 → 重复请求无 token | 表单重复提交 |
| **Redis SETNX** | `SET requestId PROCESSING NX`，已处理直接返回 | 分布式接口幂等 |
| **数据库唯一索引** | 业务唯一 ID + `UNIQUE INDEX` | 数据库操作 |
| **乐观锁** | `version` 字段，`UPDATE WHERE version=#{old}` | 更新操作 |
| **状态机** | 已处理的请求直接返回结果 | 有明确状态流转 |

**本周代码**：`IdempotentController` 用 `Request-Id` + Redis SETNX + PROCESSING/SUCCESS 状态机，防止重复提交订单。

---

### Q10: 分布式事务有哪些方案？各适合什么场景？

**答：**

| 方案 | 原理 | 一致性 | 性能 | 适用 |
|---|---|---|---|---|
| **2PC** | 协调者两阶段：询问→提交/回滚 | 强一致 | 差 | 单块应用内跨库 |
| **TCC** | Try(预留)→Confirm(确认)→Cancel(回滚) | 强一致 | 中 | 资金/库存扣减 |
| **可靠消息最终一致** | 本地事务+消息表+MQ | 最终一致 | **好** | 异步解耦（最常用） |
| **Seata AT** | 自动反向 SQL，二阶段回滚 | 接近一致 | 中 | 不想侵入代码 |
| **SAGA** | 长事务拆多个短事务+补偿 | 最终一致 | 好 | 长流程（旅行预订） |

---

## 二、Redis 高频题

### 2.1 5 大数据类型底层结构变化

| 类型 | 小数据 | 大数据 |
|---|---|---|
| String | int / embstr | raw（SDS） |
| Hash | ziplist | hashtable |
| List | quicklist | quicklist（始终） |
| Set | intset | hashtable |
| ZSet | ziplist | skiplist + dict |

### 2.2 持久化选型

| | RDB | AOF |
|---|---|---|
| **原理** | bgsave fork 子进程全量快照 | 每次写操作追加日志 |
| **优点** | 恢复快、文件小 | 数据安全、最多丢 1 秒 |
| **缺点** | 可能丢几分钟数据 | 文件大、恢复慢 |
| **建议** | **混合持久化**（Redis 4.0+）：RDB + AOF 增量 |

### 2.3 过期与淘汰

- **过期**：惰性删除（访问检查）+ 定期删除（每 100ms 抽样）
- **淘汰 8 种**：`allkeys-lru`（LRU）> `allkeys-lfu`（LFU）> `volatile-lru` > `noeviction`

### 2.4 分布式锁演进

```
手写 SET NX PX + Lua 解锁 → Redisson（WatchDog + 可重入 + 公平锁） → RedLock（多节点）
```

**本周验证（W7D2）**：`RedisDistributedLock.java` 完整实现 SET NX PX + Lua 脚本原子解锁。

### 2.5 排行榜实战（W7D1）

```java
// ZSet 排行榜：增/删/查分/查排名/TopN
redisTemplate.opsForZSet().add(RANK_KEY, userId, score);       // 更新分数
redisTemplate.opsForZSet().reverseRank(RANK_KEY, userId);       // 查排名
redisTemplate.opsForZSet().reverseRangeWithScores(RANK_KEY, 0, n-1); // Top N
```

---

## 三、MQ 高频题

### 3.1 消息可靠性全链路

```
生产者                Broker              消费者
  │ confirm/重试        │ 同步刷盘+复制       │ 手动 ACK
  ▼                     ▼                     ▼
 发送确认            持久化到磁盘           消费成功再提交
```

### 3.2 MQ 对比

| | RocketMQ | Kafka | RabbitMQ |
|---|---|---|---|
| 吞吐 | 高 | **极高** | 中 |
| 事务消息 | ✅ 原生 | ❌ | ❌ |
| 顺序消息 | ✅ 原生 | 单分区有序 | 单队列有序 |
| 适用 | 金融/电商 | 大数据/日志 | 中小型通用 |

### 3.3 消费积压处理

```
紧急：扩分区 → 加消费者 → 临时建新 Topic 暂存
根治：优化消费逻辑 → 批量消费 → 限流上游
```

---

## 四、网络与分布式速记卡片

### 4.1 TCP/UDP

| | TCP | UDP |
|---|---|---|
| 连接 | 面向连接 | 无连接 |
| 可靠 | ✅ 可靠 | ❌ 不可靠 |
| 速度 | 慢 | **快** |
| 应用 | HTTP/邮件 | 视频/DNS/游戏 |

### 4.2 三次握手 / 四次挥手

```
握手：SYN → SYN+ACK → ACK（防旧连接）
挥手：FIN → ACK → FIN → ACK（全双工，ACK 和 FIN 分开）
等 2MSL：确保 ACK 到 + 清网络残留
```

### 4.3 Cookie / Session / JWT

| | Cookie | Session | JWT |
|---|---|---|---|
| 存储 | 浏览器 | 服务端 | 客户端 |
| 分布式 | ✅ | ❌ 需共享 | ✅ 天然 |
| 失效 | 客户端删 | ✅ 服务端删 | ❌ 需黑名单 |

**本周验证（W7D5_Actual + SimpleJwtUtil）**：HMAC-SHA256 生成/验证 JWT 完整流程。

### 4.4 CAP / BASE

- **CAP**：P 必须选（网络分区避不开），C 和 A 二选一 → ZK(CP) / Eureka(AP)
- **BASE**：基本可用 + 软状态 + 最终一致性，是 AP 的实践

### 4.5 限流算法

```
令牌桶（可突发，Sentinel/Guava 在用） > 漏桶（恒定） > 滑动窗口 > 计数器
```

---

## 五、本周重点代码清单

| 序号 | 代码模块 | 关键文件 | 关键点 |
|---|---|---|---|
| 1 | Redis 排行榜 | W7D1/RedisRankService + RankController | ZSet add/rank/score/reverseRange |
| 2 | Redis 分布式锁 | W7D2/RedisDistributedLock + LockController | SET NX PX + Lua 原子解锁 |
| 3 | MQ 幂等消费 | W7D3_Actual | SETNX + 消息状态标记 + 手动 ACK |
| 4 | Wireshark 抓包 | W7D4_Actual(待补充) | TCP 三次握手/四次挥手验证 |
| 5 | JWT 生成验证 | W7D5_Actual + SimpleJwtUtil | HMAC-SHA256 + Base64 签名 |
| 6 | 接口幂等 | W7D6/IdempotentController | Request-Id + SETNX + SUCCESS/PROCESSING 状态 |
| 7 | 令牌桶限流 | W7D6_Actual(待补充) | Guava RateLimiter / 手写令牌桶 |
| 8 | 缓存一致性方案 | W7D2_Actual(待补充) | Cache Aside + 延迟双删 |
| 9 | CAP/BASE 理论 | day6-知识点背诵.md | ZK(CP) vs Eureka(AP) |
| 10 | 分布式事务方案 | day6-知识点背诵.md | 2PC/TCC/可靠消息/Seata AT/SAGA |

---

## 六、速记口诀

1. **Redis 快**：内存操作 + 单线程 + IO 多路复用 + 高效结构 + RESP 协议
2. **缓存三兄弟**：穿透布隆加空值、击穿互斥永不过期、雪崩随机高可用
3. **分布式锁坑**：锁超时（续期）/ 误删（Lua 校验）/ 单点（RedLock）/ 主从丢锁
4. **MQ 不丢**：生产 confirm → Broker 持久化 → 消费手动 ACK
5. **消息幂等**：唯一索引 / SETNX 加锁 / 乐观锁 / 消费记录表
6. **三次握手**：SYN → SYN+ACK → ACK（防止旧 SYN 浪费服务端资源）
7. **四次挥手**：FIN → ACK → FIN → ACK（全双工，ACK 和 FIN 必须分开）
8. **HTTPS**：HTTP + TLS 加密 + CA 身份认证 + MAC 防篡改
9. **CAP**：P 必选（网络分区），C（ZK）和 A（Eureka）二选一
10. **分布式事务**：2PC 强但慢、TCC 侵入强、可靠消息最常用、Seata 无侵入
