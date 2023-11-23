package com.rocket.pan.server.modules.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分享状态枚举类
 * @author 19750
 */
@AllArgsConstructor
@Getter
public enum ShareStatusEnum {

    /**
     * 正常状态
     */
    NORMAL(0, "正常状态"),
    /**
     * 有文件被删除
     */
    FILE_DELETED(1, "有文件被删除");

    private Integer code;

    private String desc;

}
