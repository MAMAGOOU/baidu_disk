package com.rocket.pan.server.modules.file.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel(value = "文件分片上传的响应实体")
public class FileChunkUploadVO implements Serializable {
    private static final long serialVersionUID = 7670192129580713809L;

    @ApiModelProperty("是否需要合并文件 0 不需要 1 需要")
    private Integer mergeFlag;

}
