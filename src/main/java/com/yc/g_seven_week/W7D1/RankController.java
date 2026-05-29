package com.yc.g_seven_week.W7D1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class RankController {

    @Autowired
    private RedisRankService redisRankService;

    // 新增/更新分数
    @PostMapping("/add")
    public String addScore(@RequestParam String userId, @RequestParam double score) {
        redisRankService.addOrUpdateScore(userId, score);
        return "添加成功";
    }

    // 查询Top N
    @GetMapping("/top")
    public Set<ZSetOperations.TypedTuple<String>> getTop(@RequestParam int n) {
        return redisRankService.getTopN(n);
    }

    // 查询用户排名和分数
    @GetMapping("/info")
    public String getUserInfo(@RequestParam String userId) {
        Long rank = redisRankService.getUserRank(userId);
        Double score = redisRankService.getUserScore(userId);
        return String.format("用户：%s，排名：%d，分数：%.0f", userId, rank + 1, score);
    }
}