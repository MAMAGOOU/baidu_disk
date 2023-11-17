package com.rocket.pan.server.modules.file.po;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 文件复制参数实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel("文件复制参数实体对象")
public class CopyFilePO implements Serializable {
    private static final long serialVersionUID = 5713445035865802696L;

    @ApiModelProperty("要复制的文件ID集合，多个使用公用分隔符隔开")
    @NotBlank(message = "请选择要复制的文件")
    private String fileIds;

    @ApiModelProperty("要转移到的目标文件夹的ID")
    @NotBlank(message = "请选择要转移到哪个文件夹下面")
    private String targetParentId;

}
