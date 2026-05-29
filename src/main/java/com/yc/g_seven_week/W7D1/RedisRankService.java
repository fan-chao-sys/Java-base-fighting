package com.yc.g_seven_week.W7D1;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisRankService {

    private static final String RANK_KEY = "game:ranking";
    private final RedisTemplate<String, String> redisTemplate;

    public RedisRankService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1. 新增/更新用户分数
    public void addOrUpdateScore(String userId, double score) {
        redisTemplate.opsForZSet().add(RANK_KEY, userId, score);
    }

    // 2. 删除用户
    public void removeUser(String userId) {
        redisTemplate.opsForZSet().remove(RANK_KEY, userId);
    }

    // 3. 查询用户分数
    public Double getUserScore(String userId) {
        return redisTemplate.opsForZSet().score(RANK_KEY, userId);
    }

    // 4. 查询用户排名（从0开始，分数越高排名越靠前）
    public Long getUserRank(String userId) {
        // 从大到小排序，排名从0开始
        return redisTemplate.opsForZSet().reverseRank(RANK_KEY, userId);
    }

    // 5. 查询Top N（分数从高到低）
    public Set<ZSetOperations.TypedTuple<String>> getTopN(int n) {
        // reverseRangeWithScores 从大到小排序，返回带分数的结果
        return redisTemplate.opsForZSet().reverseRangeWithScores(RANK_KEY, 0, n - 1);
    }
}