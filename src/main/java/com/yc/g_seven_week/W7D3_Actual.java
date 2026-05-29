import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class W7D3_Actual {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String IDEM_PREFIX = "msg:idempotent:";
    private static final long EXPIRE_TIME = 24 * 60 * 60; // 24小时


    /** 幂等消费流程图（ASCII 版）
     *
     *  消息到达消费者
     *     ↓
     *  1. 解析消息，获取 messageId
     *     ↓
     *  2. 查 Redis：是否存在 messageId
     *     ├─ 存在 → 直接返回 ack（重复消息，不处理）
     *     └─ 不存在 → 进入下一步
     *     ↓
     *  3. 用分布式锁（SETNX）锁住 messageId，防止并发重复消费
     *     ↓
     *  4. 执行业务逻辑
     *     ↓
     *  5. 消费成功：将 messageId 写入 Redis（设置过期时间）
     *     ↓
     *  6. 释放锁，返回 ack
     *
     */

    @RabbitListener(queues = "order.pay.queue")
    public void handleMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String key = IDEM_PREFIX + messageId;

        // 1. 幂等校验
        if (redisTemplate.hasKey(key)) {
            // 重复消息，直接确认
            channel.basicAck(deliveryTag, false);
            return;
        }

        // 2. 加锁（SETNX）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", 30, TimeUnit.SECONDS);
        if (!locked) {
            // 其他线程正在处理，重试或拒绝
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        try {
            // 3. 执行业务逻辑
            System.out.println("处理消息：" + messageId);
            // 模拟业务处理
            Thread.sleep(100);

            // 4. 消费成功，设置永久标记（或更长过期时间）
            redisTemplate.expire(key, EXPIRE_TIME, TimeUnit.SECONDS);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 异常：删除临时锁，让消息重试
            redisTemplate.delete(key);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
