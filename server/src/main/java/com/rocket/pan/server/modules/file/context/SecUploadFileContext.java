package com.rocket.pan.server.modules.file.context;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件妙传的上下文对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class SecUploadFileContext implements Serializable {
    private static final long serialVersionUID = 865765374680289146L;

    /**
     * 文件的父ID
     */
    private Long parentId;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件的唯一标识
     */
    private String identifier;

    /**
     * 当前登录用的ID
     */
    private Long userId;
}
