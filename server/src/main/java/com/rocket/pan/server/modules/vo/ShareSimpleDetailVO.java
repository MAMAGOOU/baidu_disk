package com.rocket.pan.server.modules.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rocket.pan.web.serializer.IdEncryptSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询分享简单详情返回实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel("查询分享简单详情返回实体对象")
public class ShareSimpleDetailVO implements Serializable {
    private static final long serialVersionUID = -244174348108049506L;
    @ApiModelProperty("分享ID")
    @JsonSerialize(using = IdEncryptSerializer.class)
    private Long shareId;

    @ApiModelProperty("分享名称")
    private String shareName;

    @ApiModelProperty("分享人信息")
    private ShareUserInfoVO shareUserInfoVO;
}
