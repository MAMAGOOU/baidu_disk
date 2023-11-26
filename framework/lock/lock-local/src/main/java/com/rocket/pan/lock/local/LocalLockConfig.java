package com.rocket.pan.lock.local;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * 本地锁配置类
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
@Log4j2
public class LocalLockConfig {

    /**
     * 配置本地锁注册器
     *
     * @return
     */
    @Bean
    public LockRegistry localLockRegistry() {
        LockRegistry lockRegistry = new DefaultLockRegistry();
        log.info("the local lock is loaded successfully.");
        return lockRegistry;
    }
}
