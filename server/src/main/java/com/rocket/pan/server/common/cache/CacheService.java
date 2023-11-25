package com.rocket.pan.server.common.cache;

import java.io.Serializable;

/**
 * 支持业务缓存的顶级Service接口
 * <p>
 * 最顶级的接口一定要定义最简单的，不能定义为很复杂的结构
 *
 * @param <V>
 * @author 19750
 */
public interface CacheService<V> {
    /**
     * 根据ID查询实体
     *
     * @param id
     * @return
     */
    V getById(Serializable id);

    /**
     * 根据ID来更新缓存信息
     *
     * @param id
     * @param entity
     * @return
     */
    boolean updateById(Serializable id, V entity);

    /**
     * 根据ID来删除缓存信息
     *
     * @param id
     * @return
     */
    boolean removeById(Serializable id);
}
