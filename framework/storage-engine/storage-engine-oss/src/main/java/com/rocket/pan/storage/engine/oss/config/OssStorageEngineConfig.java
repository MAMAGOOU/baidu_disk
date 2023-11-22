package com.rocket.pan.storage.engine.oss.config;

import com.aliyun.oss.OSSClient;
import com.rocket.pan.core.exception.RPanFrameworkException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * OSS文件存储引擎配置类
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Data
@ConfigurationProperties(prefix = "com.rocket.pan.storage.engine.oss")
public class OssStorageEngineConfig {
    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

    private Boolean autoCreateBucket = Boolean.TRUE;

    /**
     * 注入OSS操作客户端对象
     *
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public OSSClient ossClient() {
        if (StringUtils.isAnyBlank(getEndpoint(), getAccessKeyId(), getAccessKeySecret(), getBucketName())) {
            throw new RPanFrameworkException("the oss config is missed!");
        }
        return new OSSClient(getEndpoint(), getAccessKeyId(), getAccessKeySecret());
    }
}
