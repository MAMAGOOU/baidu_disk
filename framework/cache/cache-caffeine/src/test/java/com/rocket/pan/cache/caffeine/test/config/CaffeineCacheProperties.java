package com.rocket.pan.cache.caffeine.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 19750
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "com.rocket.pan.cache.caffeine")
public class CaffeineCacheProperties {

    /**
     * 缓存初始容量
     * com.rocket.pan.cache.caffeine.init-cache-capacity
     */
    private Integer initCacheCapacity = 256;

    private Long maxCacheCapacity = 10000L;

    private Boolean allowNullValue = Boolean.TRUE;
}
