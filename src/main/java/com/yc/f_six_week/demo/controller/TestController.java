package com.yc.f_six_week.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/success")
    public String success() throws InterruptedException {
        Thread.sleep(100); // 模拟业务耗时
        return "调用成功";
    }

    @GetMapping("/error")
    public String error() {
        throw new RuntimeException("接口调用出错啦！");
    }
}