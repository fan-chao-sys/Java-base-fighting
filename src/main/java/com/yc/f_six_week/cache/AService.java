package com.yc.f_six_week.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

// AService.java
@Service
public class AService {
    @Autowired
    private B2Service bService;

    public void test() {
        System.out.println("AService 调用 BService: " + bService);
    }
}

// 运行结果：正常启动，控制台打印对象引用，循环依赖被成功解决