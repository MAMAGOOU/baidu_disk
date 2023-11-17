package com.rocket.pan.server.modules.file.vo;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.rocket.pan.web.serializer.IdEncryptSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 面包屑列表展示实体
 *
 * @author 19750
 * @version 1.0
 */
@ApiModel("面包屑列表展示实体")
@Data
public class BreadcrumbVO implements Serializable {
    private static final long serialVersionUID = -6113151935665730951L;

    @ApiModelProperty("文件ID")
    @JsonSerialize(using = IdEncryptSerializer.class)
    private Long id;

    @ApiModelProperty("父文件夹ID")
    @JsonSerialize(using = IdEncryptSerializer.class)
    private Long parentId;

    @ApiModelProperty("文件夹名称")
    private String name;

    /**
     * 实体转换
     *
     * @param record
     * @return
     */
    public static BreadcrumbVO transfer(RPanUserFile record) {
        BreadcrumbVO vo = new BreadcrumbVO();

        if (ObjectUtil.isNotNull(record)) {
            vo.setId(record.getFileId());
            vo.setParentId(record.getParentId());
            vo.setName(record.getFilename());
        }

        return vo;
    }

}
