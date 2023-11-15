package com.rocket.pan.storage.engine.local.config;

import com.rocket.pan.core.utils.FileUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 19750
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "com.rocket.pan.storage.engine.local")
public class LocalStorageEngineConfig {
    /**
     * 实际存放路径的前缀
     */
    private String rootFilePath = FileUtils.generateDefaultStoreFileRealPath();

    /**
     * 实际存放文件分片的路径的前缀
     */
    private String rootFileChunkPath = FileUtils.generateDefaultStoreFileChunkRealPath();
}
