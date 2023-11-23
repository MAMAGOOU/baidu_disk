package com.rocket.pan.server.modules.share.context;

import com.rocket.pan.server.modules.share.entity.RPanShare;
import com.rocket.pan.server.modules.vo.ShareDetailVO;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询分享详情的上下文实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class QueryShareDetailContext implements Serializable {
    /**
     * 对应的分享ID
     */
    private Long shareId;

    /**
     * 分享实体
     */
    private RPanShare record;

    /**
     * 分享详情的VO对象
     */
    private ShareDetailVO vo;

}
