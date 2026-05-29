package com.yc.f_six_week.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

// 启动类
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);
        AService aService = context.getBean(AService.class);
        B2Service bService = context.getBean(B2Service.class);
        aService.test();
        bService.test();
    }
}
