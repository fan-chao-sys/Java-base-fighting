package com.yc.g_seven_week.W7D6;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;


// 设计一个接口幂等方案，写出核心代码逻辑
@RestController
@RequestMapping("/api/order")
public class IdempotentController {

    private static final String IDEM_PREFIX = "idempotent:order:";
    private static final long EXPIRE_TIME = 5; // 5分钟过期

    private final StringRedisTemplate redisTemplate;

    public IdempotentController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam String orderId,
                              @RequestHeader("Request-Id") String requestId) {
        String key = IDEM_PREFIX + requestId;

        // 1. 幂等校验：判断 requestId 是否已处理
        String status = redisTemplate.opsForValue().get(key);
        if (status != null) {
            if ("SUCCESS".equals(status)) {
                return "订单已创建，请勿重复提交";
            } else if ("PROCESSING".equals(status)) {
                return "订单处理中，请稍后再试";
            }
        }

        // 2. 标记为处理中（SETNX，防止并发重复提交）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSING", 1, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            return "订单处理中，请稍后再试";
        }

        try {
            // 3. 执行业务逻辑（创建订单）
            String result = doCreateOrder(orderId);

            // 4. 标记为处理成功，设置较长过期时间
            redisTemplate.opsForValue().set(key, "SUCCESS", EXPIRE_TIME, TimeUnit.MINUTES);
            return result;
        } catch (Exception e) {
            // 异常：删除标记，允许重试
            redisTemplate.delete(key);
            throw new RuntimeException("订单创建失败，请重试");
        }
    }

    // 模拟创建订单的业务逻辑
    private String doCreateOrder(String orderId) {
        return "订单创建成功：" + orderId;
    }
}