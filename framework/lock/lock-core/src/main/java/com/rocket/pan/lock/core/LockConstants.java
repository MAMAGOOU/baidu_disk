package com.rocket.pan.lock.core;

/**
 * 锁相关公用常量类
 *
 * @author 19750
 * @version 1.0
 */
public interface LockConstants {
    /**
     * 公用锁名称
     */
    String R_PAN_LOCK = "r-pan-lock";

    /**
     * 公用lock的path
     * 主要是针对zookeeper等节点型软件
     */
    String R_PAN_LOCK_PATH = "/r-pan-lock";


}
