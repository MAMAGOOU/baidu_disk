package com.rocket.pan.server.modules.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rocket.pan.web.serializer.IdEncryptSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建分享链接的返回实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
@ApiModel(value = "创建分享链接的返回实体对象")
public class RPanShareUrlVO implements Serializable {
    private static final long serialVersionUID = 3468789641541361147L;

    @JsonSerialize(using = IdEncryptSerializer.class)
    @ApiModelProperty("分享链接的ID")
    private Long shareId;

    @ApiModelProperty("分享链接的名称")
    private String shareName;

    @ApiModelProperty("分享链接的URL")
    private String shareUrl;

    @ApiModelProperty("分享链接的分享码")
    private String shareCode;

    @ApiModelProperty("分享链接的状态")
    private Integer shareStatus;

}
