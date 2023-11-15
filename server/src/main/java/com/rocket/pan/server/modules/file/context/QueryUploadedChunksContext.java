package com.rocket.pan.server.modules.file.context;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询用户已经上传的分片列表的上下文信息实体
 *
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel(value = "查询用户已经上传的分片列表的上下文信息实体")
public class QueryUploadedChunksContext implements Serializable {
    private static final long serialVersionUID = -2219913977857676171L;

    /**
     * 当前登录的用户ID
     */
    private Long userId;
    /**
     * 文件的唯一标识
     */
    private String identifier;
}
