package com.rocket.pan.server.modules.user.context;

import lombok.Data;

/**
 * 重置用户密码上下文信息实体
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class ResetPasswordContext {
    private static final long serialVersionUID = 6483482439489859204L;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户新密码
     */
    private String password;

    /**
     * 重置密码的token信息
     */
    private String token;
}
