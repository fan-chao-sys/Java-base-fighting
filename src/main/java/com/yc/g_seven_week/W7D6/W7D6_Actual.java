package com.yc.g_seven_week.W7D6;

import com.yc.Xcommon.TokenBucketRateLimiter;

public class W7D6_Actual {

    // 手写一个简单的令牌桶限流器
    public static void main(String[] args) throws InterruptedException {
        // 桶容量10，每秒生成2个令牌
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 2);

        // 模拟20个并发请求
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                if (limiter.tryAcquire()) {
                    System.out.println(Thread.currentThread().getName() + " 请求通过");
                } else {
                    System.out.println(Thread.currentThread().getName() + " 请求被限流");
                }
            }).start();
            Thread.sleep(100);
        }
    }
}
