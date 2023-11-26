package com.rocket.pan.lock.zookeeper;

import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.zookeeper.config.CuratorFrameworkFactoryBean;
import org.springframework.integration.zookeeper.lock.ZookeeperLockRegistry;

/**
 * 基于ZK的分布式锁配置
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
@Log4j2
public class ZooKeeperLockConfig {
    @Autowired
    private ZooKeeperLockProperties properties;

    /**
     * 配置zk的客户端
     *
     * @return
     */
    @Bean
    public CuratorFrameworkFactoryBean curatorFrameworkFactoryBean() {
        return new CuratorFrameworkFactoryBean(properties.getHost());
    }

    /**
     * 配置zk分布式锁的注册器
     *
     * @param curatorFramework
     * @return
     */
    @Bean
    public LockRegistry zookeeperLockRegistry(CuratorFramework curatorFramework) {
        ZookeeperLockRegistry lockRegistry = new ZookeeperLockRegistry(curatorFramework);
        log.info("the zookeeper lock is loaded successfully!");
        return lockRegistry;
    }
}
