package com.rocket.pan.server.modules.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分享类型枚举
 * @author 19750
 */
@Getter
@AllArgsConstructor
public enum ShareTypeEnum {
    /**
     * 有提取码
     */
    NEED_SHARE_CODE(0, "有提取码");
    private Integer code;
    private String desc;
}
