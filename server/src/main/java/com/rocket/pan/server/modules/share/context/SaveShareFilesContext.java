package com.rocket.pan.server.modules.share.context;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 保存分享和分享文件关联关系的上下文实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class SaveShareFilesContext implements Serializable {
    private static final long serialVersionUID = -5668591757498143170L;

    /**
     * 分享的ID
     */
    private Long shareId;

    /**
     * 分享对应的文件的ID集合
     */
    private List<Long> shareFileIdList;

    /**
     * 当前登录的用户ID
     */
    private Long userId;

}
