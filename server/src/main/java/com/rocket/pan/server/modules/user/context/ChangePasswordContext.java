package com.rocket.pan.server.modules.user.context;

import com.rocket.pan.server.modules.user.entity.RPanUser;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改密码上下文信息实体
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class ChangePasswordContext implements Serializable {

    /**
     * 当前登录的用户ID
     */
    private Long userId;

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 当前登录用户的实体信息
     */
    private RPanUser entity;
}
