package com.yc.f_six_week.demo.anotation;

import java.lang.annotation.*;

/**
 * 自定义日志注解，用于标记需要打印日志的方法
 */
@Target(ElementType.METHOD) // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface Log {
    // 日志描述（可选，记录方法功能）
    String value() default "";
}