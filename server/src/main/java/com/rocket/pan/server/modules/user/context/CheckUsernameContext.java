package com.rocket.pan.server.modules.user.context;

import lombok.Data;

import java.io.Serializable;

/**
 * 校验用户名称上下文对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class CheckUsernameContext implements Serializable {
    private static final long serialVersionUID = -7117844539768126736L;

    /**
     * 用户名称
     */
    private String username;
}
