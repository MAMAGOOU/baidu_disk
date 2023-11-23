package com.rocket.pan.server.modules.recycle.po;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 文件还原参数实体
 *
 * @author 19750
 * @version 1.0
 */
@ApiModel("文件还原参数实体")
@Data
public class RestorePO implements Serializable {
    private static final long serialVersionUID = -8600005249933040664L;

    @ApiModelProperty(value = "要还原的文件ID集合，多个使用公用分割符分隔", required = true)
    @NotBlank(message = "请选择要还原的文件")
    private String fileIds;
}
