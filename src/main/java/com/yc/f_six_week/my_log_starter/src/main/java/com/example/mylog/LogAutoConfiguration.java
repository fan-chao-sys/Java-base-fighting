package com.yc.f_six_week.my_log_starter.src.main.java.com.example.mylog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean // 当容器中没有 LogService 时，才创建
    public LogService logService() {
        // 这里可以读取配置文件中的属性，简化演示直接写死
        return new LogService("MY-LOG");
    }
}