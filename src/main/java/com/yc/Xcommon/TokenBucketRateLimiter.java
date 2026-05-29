package com.yc.Xcommon;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucketRateLimiter {
    // 桶的最大容量
    private final long capacity;
    // 令牌生成速率（每秒生成多少个）
    private final long rate;
    // 当前桶中的令牌数
    private AtomicLong tokens;
    // 上次补充令牌的时间戳
    private long lastRefillTime;

    /**
     * @param capacity 桶的最大容量
     * @param rate 令牌生成速率（每秒生成的令牌数）
     */
    public TokenBucketRateLimiter(long capacity, long rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.tokens = new AtomicLong(capacity); // 初始桶是满的
        this.lastRefillTime = System.nanoTime();
    }

    /**
     * 尝试获取令牌
     * @return true：获取成功，false：被限流
     */
    public synchronized boolean tryAcquire() {
        // 1. 补充令牌
        long now = System.nanoTime();
        // 计算距离上次补充令牌的时间差（秒）
        double elapsedTime = (now - lastRefillTime) / 1e9;
        // 计算这段时间应该生成的令牌数
        long newTokens = (long) (elapsedTime * rate);
        if (newTokens > 0) {
            // 补充令牌，不超过桶的容量
            tokens.set(Math.min(capacity, tokens.get() + newTokens));
            lastRefillTime = now;
        }

        // 2. 尝试获取令牌
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false;
    }


}