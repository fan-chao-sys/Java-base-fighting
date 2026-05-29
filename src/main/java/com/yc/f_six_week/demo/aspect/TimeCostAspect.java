package com.yc.f_six_week.demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimeCostAspect {

    // 切面表达式：拦截 controller 包下所有 public 方法
    private static final String POINT_CUT = "execution(public * com.example.demo.controller.*.*(..))";

    /**
     * @Around：环绕通知，可统计方法执行耗时
     * 特点：
     * 1. 可在方法前后都执行逻辑
     * 2. 可控制方法是否执行
     * 3. 可修改返回值
     */
    @Around(POINT_CUT)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            System.out.printf("[耗时统计] 方法 %s 执行成功，耗时：%dms%n", methodName, cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            System.out.printf("[耗时统计] 方法 %s 执行异常，耗时：%dms%n", methodName, cost);
            throw e; // 异常继续抛出，交给 @AfterThrowing 处理
        }
    }

    /**
     * @AfterThrowing：异常通知，方法抛出异常时执行
     * 特点：
     * 1. 只能捕获目标方法抛出的异常
     * 2. 不能修改返回值
     */
    @AfterThrowing(pointcut = POINT_CUT, throwing = "e")
    public void afterThrowing(Throwable e) {
        System.out.printf("[异常通知] 方法执行出错，异常信息：%s%n", e.getMessage());
    }
}