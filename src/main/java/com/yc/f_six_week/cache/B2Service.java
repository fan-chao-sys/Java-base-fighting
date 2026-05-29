package com.yc.f_six_week.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// BService.java
@Service
public class B2Service {
    private final A2Service aService;

    // 构造器注入 AService
    public B2Service(A2Service aService) {
        this.aService = aService;
    }

    public void test() {
        System.out.println("BService 调用 AService: " + aService);
    }
}

// // 运行结果：启动失败，报错：
////Requested bean is currently in creation: Is there an unresolvable circular reference?