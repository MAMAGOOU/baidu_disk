package com.rocket.pan.server.common.config;

import com.rocket.pan.server.common.interceptor.BloomFilterInterceptor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 项目拦截器配置类
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
@Log4j2
public class InterceptorConfig implements WebMvcConfigurer {
    @Autowired
    private List<BloomFilterInterceptor> interceptorList;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (CollectionUtils.isNotEmpty(interceptorList)) {
            interceptorList.stream().forEach(bloomFilterInterceptor -> {
                registry.addInterceptor(bloomFilterInterceptor)
                        .addPathPatterns(bloomFilterInterceptor.getPathPatterns())
                        .excludePathPatterns(bloomFilterInterceptor.getExcludePatterns());
                log.info("add bloomFilterInterceptor {} finish.", bloomFilterInterceptor.getName());
            });
        }
    }
}
