package com.yc.g_seven_week.W7D2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class LockController {

    @Autowired
    private RedisDistributedLock distributedLock;

    private static final String LOCK_NAME = "order:pay";

    @GetMapping("/pay")
    public String payOrder() {
        // 生成唯一请求ID（可以用 UUID 或 JWT 中的用户ID+请求ID）
        String requestId = UUID.randomUUID().toString();
        // 尝试加锁，过期时间30秒
        boolean locked = distributedLock.tryLock(LOCK_NAME, requestId, 30000);
        if (!locked) {
            return "系统繁忙，请稍后再试";
        }
        try {
            // 模拟业务处理
            System.out.println("执行业务逻辑，请求ID：" + requestId);
            Thread.sleep(5000);
            return "支付成功";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "系统异常";
        } finally {
            // 解锁
            distributedLock.unlock(LOCK_NAME, requestId);
        }
    }
}