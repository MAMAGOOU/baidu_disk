package com.rocket.pan.server.modules.file.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @author 19750
 */

@AllArgsConstructor
@Getter
public enum FolderFlagEnum {
    /**
     * 非文件夹
     */
    NO(0),
    /**
     * 是文件夹
     */
    YES(1);
    private Integer code;
}
