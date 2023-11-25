package com.rocket.pan.server.modules.user.service.cache;

import com.rocket.pan.cache.core.constants.CacheConstants;
import com.rocket.pan.server.common.cache.AnnotationCacheService;
import com.rocket.pan.server.modules.user.entity.RPanUser;
import com.rocket.pan.server.modules.user.mapper.RPanUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 用户模块缓存业务处理类
 *
 * @author 19750
 * @version 1.0
 */
@Component(value = "userAnnotationCacheService")
public class UserCacheService implements AnnotationCacheService<RPanUser> {
    @Autowired
    private RPanUserMapper mapper;

    /**
     * 根据ID查询实体
     *
     * @param id
     * @return
     */
    @Override
    @Cacheable(cacheNames = CacheConstants.R_PAN_CACHE_NAME, keyGenerator = "userIdKeyGenerator", sync = true)
    public RPanUser getById(Serializable id) {
        return mapper.selectById(id);
    }

    /**
     * 根据ID来更新缓存信息
     *
     * @param id
     * @param entity
     * @return
     */
    @Override
    @CacheEvict(cacheNames = CacheConstants.R_PAN_CACHE_NAME, keyGenerator = "userIdKeyGenerator")
    public boolean updateById(Serializable id, RPanUser entity) {
        return mapper.updateById(entity) == 1;
    }

    /**
     * 根据ID来删除缓存信息
     *
     * @param id
     * @return
     */
    @Override
    @CacheEvict(cacheNames = CacheConstants.R_PAN_CACHE_NAME, keyGenerator = "userIdKeyGenerator")
    public boolean removeById(Serializable id) {
        return mapper.deleteById(id) == 1;
    }
}
