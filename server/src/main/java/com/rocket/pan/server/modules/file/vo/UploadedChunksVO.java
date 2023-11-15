package com.rocket.pan.server.modules.file.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询用户已上传的文件分片列表返回实体
 *
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel("查询用户已上传的文件分片列表返回实体")
public class UploadedChunksVO implements Serializable {

    private static final long serialVersionUID = 8694674586602329820L;

    @ApiModelProperty("已上传的分片编号列表")
    private List<Integer> uploadedChunks;
}
