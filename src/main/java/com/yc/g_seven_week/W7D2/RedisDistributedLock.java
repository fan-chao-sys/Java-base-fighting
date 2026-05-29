package com.yc.g_seven_week.W7D2;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedLock {

    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final long DEFAULT_TIMEOUT = 30000; // 默认过期时间30秒
    private static final String UNLOCK_LUA_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 加锁
     * @param lockName 锁名称
     * @param requestId 请求唯一标识（锁持有者ID）
     * @param timeout 过期时间（毫秒）
     * @return 是否加锁成功
     */
    public boolean tryLock(String lockName, String requestId, long timeout) {
        String key = LOCK_KEY_PREFIX + lockName;
        // SET key value NX PX timeout 原子命令
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, requestId, timeout, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 解锁（Lua 脚本保证原子性）
     * @param lockName 锁名称
     * @param requestId 请求唯一标识
     * @return 是否解锁成功
     */
    public boolean unlock(String lockName, String requestId) {
        String key = LOCK_KEY_PREFIX + lockName;
        // 执行 Lua 脚本
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), requestId);
        return result != null && result == 1;
    }
}