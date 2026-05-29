package com.yc.f_six_week.cache;

import org.springframework.stereotype.Service;

// AService.java
@Service
public class A2Service {
    private final B2Service bService;

    // 构造器注入 BService
    public A2Service(B2Service bService) {
        this.bService = bService;
    }

    public void test() {
        System.out.println("AService 调用 BService: " + bService);
    }
}


