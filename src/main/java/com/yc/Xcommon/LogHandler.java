package com.yc.Xcommon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

// 3. 日志处理器（InvocationHandler）
public class LogHandler implements InvocationHandler {
    // 被代理的目标对象
    private final Object target;

    public LogHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 方法调用前：打印日志
        System.out.println("[日志] 方法 " + method.getName() + " 开始执行，参数：" + (args == null ? "无" : java.util.Arrays.toString(args)));

        // 执行目标方法
        Object result = method.invoke(target, args);

        // 方法调用后：打印日志
        System.out.println("[日志] 方法 " + method.getName() + " 执行结束");
        return result;
    }
}
