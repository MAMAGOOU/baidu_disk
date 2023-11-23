package com.rocket.pan.server.common.config;

import com.rocket.pan.core.constants.RPanConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 19750
 * @version 1.0
 */
@Component
@Data
@ConfigurationProperties(prefix = "com.rocket.pan.server")
public class PanServerConfig {
    /**
     * 文件分片的过期天数
     */
    private Integer chunkFileExpirationDays = RPanConstants.ONE_INT;

    /**
     * 分享链接的前缀
     */
    private String sharePrefix = "http://127.0.0.1:8080/share/";
}
