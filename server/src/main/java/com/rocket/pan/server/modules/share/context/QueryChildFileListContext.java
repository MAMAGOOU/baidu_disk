package com.rocket.pan.server.modules.share.context;

import com.rocket.pan.server.modules.share.entity.RPanShare;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询下一级列表上下文实体信息
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class QueryChildFileListContext implements Serializable {
    private static final long serialVersionUID = 884255624221527918L;

    /**
     * 分享的ID
     */
    private Long shareId;

    /**
     * 父文件夹的ID
     */
    private Long parentId;

    /**
     * 分享对应的实体信息
     */
    private RPanShare record;
}
