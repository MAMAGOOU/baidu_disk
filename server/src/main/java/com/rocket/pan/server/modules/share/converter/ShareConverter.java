package com.rocket.pan.server.modules.share.converter;

import com.rocket.pan.server.modules.share.context.CreateShareUrlContext;
import com.rocket.pan.server.modules.share.po.CreateShareUrlPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

/**
 * 分享模块实体转化工具类
 *
 * @author 19750
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface ShareConverter {
    @Mapping(target = "userId", expression = "java(com.rocket.pan.server.common.utils.UserIdUtil.get())")
    CreateShareUrlContext createShareUrlPO2CreateShareUrlContext(CreateShareUrlPO createShareUrlPO);

}
