package com.yc.f_six_week.demo.aspect;

import com.yc.f_six_week.demo.anotation.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class LogAspect {

    /**
     * 切面表达式：拦截所有标记了 @Log 注解的方法
     */
    @Around("@annotation(com.yc.f_six_week.demo.anotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);
        String description = logAnnotation.value();

        // 2. 打印方法执行前日志
        System.out.println("===== 方法日志开始 =====");
        System.out.println("方法描述：" + description);
        System.out.println("方法名称：" + method.getName());
        System.out.println("参数列表：" + joinPoint.getArgs());

        long start = System.currentTimeMillis();
        Object result = null;
        try {
            // 3. 执行目标方法
            result = joinPoint.proceed();
            // 4. 打印方法执行成功日志
            System.out.println("执行结果：" + result);
            System.out.println("执行耗时：" + (System.currentTimeMillis() - start) + "ms");
            System.out.println("===== 方法日志结束（成功）=====\n");
            return result;
        } catch (Throwable e) {
            // 5. 打印方法执行异常日志
            System.out.println("执行异常：" + e.getMessage());
            System.out.println("===== 方法日志结束（失败）=====\n");
            throw e;
        }
    }
}