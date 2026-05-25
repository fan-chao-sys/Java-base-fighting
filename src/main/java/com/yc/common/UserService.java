package com.yc.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// 1. 业务接口
public interface UserService {
    void addUser(String name);
    void deleteUser(int id);
}

