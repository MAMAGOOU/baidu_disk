package com.rocket.pan.server.modules.enums;

import com.rocket.pan.core.constants.RPanConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 分享日期类型枚举类
 *
 * @author 19750
 */
@AllArgsConstructor
@Getter
public enum ShareDayTypeEnum {
    /**
     * 分享有效时间
     */
    PERMANENT_VALIDITY(0, 0, "永久有效"),
    SEVEN_DAYS_VALIDITY(1, 7, "七天有效"),
    THIRTY_DAYS_VALIDITY(2, 30, "三十天有效");

    private Integer code;

    private Integer days;

    private String desc;

    /**
     * 根据穿过来的分享天数的code获取对应的分享天数的数值
     *
     * @param code
     * @return
     */
    public static Integer getShareDayByCode(Integer code) {
        if (Objects.isNull(code)) {
            return RPanConstants.MINUS_ONE_INT;
        }
        for (ShareDayTypeEnum value : values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value.getDays();
            }
        }
        return RPanConstants.MINUS_ONE_INT;
    }

}
