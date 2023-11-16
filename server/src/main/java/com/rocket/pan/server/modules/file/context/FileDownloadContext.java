package com.rocket.pan.server.modules.file.context;

import lombok.Data;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件下载的上下文实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class FileDownloadContext {
    /**
     * 文件ID
     */
    private Long fileId;
    /**
     * 请求响应对象
     */
    private HttpServletResponse response;
    /**
     * 当前登录的用户ID
     */
    private Long userId;
}
