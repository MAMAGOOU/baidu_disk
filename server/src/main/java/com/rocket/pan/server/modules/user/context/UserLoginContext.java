package com.rocket.pan.server.modules.user.context;

import com.rocket.pan.server.modules.user.entity.RPanUser;
import lombok.Data;

/**
 * 用户登录业务上下文实体
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class UserLoginContext {
    private static final long serialVersionUID = -3754570303177237029L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户实体对象
     */
    private RPanUser entity;

    /**
     * 登陆成功之后的凭证信息
     */
    private String accessToken;
}
