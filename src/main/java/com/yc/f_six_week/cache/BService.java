package com.yc.f_six_week.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// BService.java
@Service
public class BService {
    @Autowired
    private AService aService;

    public void test() {
        System.out.println("BService 调用 AService: " + aService);
    }
}
