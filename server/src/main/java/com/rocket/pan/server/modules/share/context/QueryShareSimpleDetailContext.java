package com.rocket.pan.server.modules.share.context;

import com.rocket.pan.server.modules.share.entity.RPanShare;
import com.rocket.pan.server.modules.vo.ShareSimpleDetailVO;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询分享简单详情上下文实体信息
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class QueryShareSimpleDetailContext implements Serializable {
    /**
     * 分享的ID
     */
    private Long shareId;

    /**
     * 分享对应的实体信息
     */
    private RPanShare record;

    /**
     * 简单分享详情的VO对象
     */
    private ShareSimpleDetailVO vo;
}
