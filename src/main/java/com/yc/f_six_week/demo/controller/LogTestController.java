package com.yc.f_six_week.demo.controller;

import com.yc.f_six_week.demo.anotation.Log;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogTestController {

    @Log(value = "用户登录接口")
    @GetMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        return "用户 " + username + " 登录成功";
    }

    @Log(value = "抛出异常测试")
    @GetMapping("/log-error")
    public String logError() {
        throw new RuntimeException("测试异常日志打印");
    }
}