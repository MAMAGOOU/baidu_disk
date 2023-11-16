package com.rocket.pan.storage.engine.core.context;

import lombok.Data;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * 文件读取的上下文实体信息
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class ReadFileContext implements Serializable {
    private static final long serialVersionUID = 2506771761529717302L;

    /**
     * 文件存储的真实路径
     */
    private String realPath;
    /**
     * 文件输出流
     */
    private OutputStream outputStream;
}
