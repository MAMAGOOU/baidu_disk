package com.rocket.pan.lock.core.key;

import com.rocket.pan.lock.core.LockContext;

/**
 * 锁的key的生成器顶级接口
 *
 * @author 19750
 * @version 1.0
 */
public interface KeyGenerator {
    /**
     * 生成锁的key
     * @param lockContext
     * @return
     */
    String generateKey(LockContext lockContext);
}
