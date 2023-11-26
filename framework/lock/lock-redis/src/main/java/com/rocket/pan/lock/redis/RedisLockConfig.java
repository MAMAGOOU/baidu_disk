package com.rocket.pan.lock.redis;

import com.rocket.pan.lock.core.LockConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * 基于redis使用分布式锁
 * 该方案集成了spring-data-redis，配置项目可以直接复用原来配置，不必重复造轮子
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
@Log4j2
public class RedisLockConfig {
    @Bean
    public LockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory) {
        RedisLockRegistry lockRegistry = new RedisLockRegistry(redisConnectionFactory,
                LockConstants.R_PAN_LOCK);
        log.info("redis lock is loaded successfully!");
        return lockRegistry;
    }
}
