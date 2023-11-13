package com.rocket.pan.cache.redis.test.instance;

import com.rocket.pan.cache.core.constants.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Cache注解测试实体
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Slf4j
public class CacheAnnotationTester {


    @Cacheable(cacheNames = CacheConstants.R_PAN_CACHE_NAME, key = "#name", sync = true)
    public String testCacheable(String name) {
        log.info("call com.rocket.pan.cache.caffeine.test.instance.CacheAnnotationTester.testCacheable, param is {}", name);
        return new StringBuilder("hello ").append(name).toString();
    }
}
