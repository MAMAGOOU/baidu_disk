package com.rocket.pan.server.modules.share.service.cache;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocket.pan.server.common.cache.AbstractManualCacheService;
import com.rocket.pan.server.modules.share.entity.RPanShare;
import com.rocket.pan.server.modules.share.mapper.RPanShareMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 手动缓存实现分享业务的查询等操作
 *
 * @author 19750
 * @version 1.0
 */
@Component(value = "shareCacheService")
public class ShareCacheService extends AbstractManualCacheService<RPanShare> {

    @Autowired
    private RPanShareMapper mapper;

    @Override
    protected BaseMapper<RPanShare> getBaseMapper() {
        return mapper;
    }

    /**
     * 获取缓存key的模版信息
     *
     * @return
     */
    @Override
    public String getKeyFormat() {
        return "SHARE:ID:%s";
    }
}
