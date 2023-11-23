package com.rocket.pan.server.modules.vo;

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
@ApiModel("分享者信息返回实体对象")
@Data
public class ShareUserInfoVO implements Serializable {

    private static final long serialVersionUID = 5739630033108250153L;

    @ApiModelProperty("分享者的ID")
    @JsonSerialize(using = IdEncryptSerializer.class)
    private Long userId;

    @ApiModelProperty("分享者的名称")
    private String username;
}
