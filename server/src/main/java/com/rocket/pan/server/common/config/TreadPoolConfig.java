package com.rocket.pan.server.common.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
public class TreadPoolConfig {
    @Bean(name = "eventListenerTaskExecutor")
    public ThreadPoolTaskExecutor eventListenerTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setKeepAliveSeconds(200);
        taskExecutor.setQueueCapacity(2048);
        taskExecutor.setThreadNamePrefix("event-listener-thread");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return taskExecutor;
    }
}
