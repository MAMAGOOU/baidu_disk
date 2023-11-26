package com.rocket.pan.lock.zookeeper;

import com.rocket.pan.lock.core.LockConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * zk属性配置
 *
 * @author 19750
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "com.rocket.pan.lock.zookeeper")
public class ZooKeeperLockProperties {
    /**
     * zk链接地址，多个用逗号隔开
     */
    private String host = "39.99.156.23:2182";
    /**
     * zk分布式锁的根路径
     */
    private String rootPath = LockConstants.R_PAN_LOCK_PATH;
}
