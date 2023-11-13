package com.rocket.pan.schedule;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时模块配置类
 * 配置定时器配置器
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
public class ScheduleConfig {
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        return taskScheduler;
    }
}
