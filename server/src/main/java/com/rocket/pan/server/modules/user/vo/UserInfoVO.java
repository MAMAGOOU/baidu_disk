package com.rocket.pan.server.modules.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rocket.pan.web.serializer.IdEncryptSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel("用户基本信息实体")
public class UserInfoVO implements Serializable {
    private static final long serialVersionUID = 831556981609248699L;

    @ApiModelProperty("用户名称")
    private String username;

    @ApiModelProperty("用户根目录的加密ID")
    @JsonSerialize(using = IdEncryptSerializer.class)
    private Long rootFileId;

    @ApiModelProperty("用户根目录名称")
    private String rootFilename;
}
