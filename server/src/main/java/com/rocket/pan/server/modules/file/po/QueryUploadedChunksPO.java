package com.rocket.pan.server.modules.file.po;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel(value = "查询用户已上传分片列表的参数实体")
public class QueryUploadedChunksPO implements Serializable {
    private static final long serialVersionUID = 866722676187500143L;

    @ApiModelProperty(value = "文件的唯一标识", required = true)
    @NotBlank(message = "文件唯一标识不能为空")
    private String identifier;
}
