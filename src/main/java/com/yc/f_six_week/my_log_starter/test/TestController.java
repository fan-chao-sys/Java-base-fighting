package com.yc.f_six_week.my_log_starter.test;

import com.yc.f_six_week.my_log_starter.src.main.java.com.example.mylog.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    private LogService logService;

    @GetMapping("/test-log")
    public String testLog() {
        logService.log("自定义 Starter 日志打印成功！");
        return "OK";
    }
}