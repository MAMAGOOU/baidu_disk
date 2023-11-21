package com.rocket.pan.server.modules.user.service;

import com.rocket.pan.server.modules.user.context.*;
import com.rocket.pan.server.modules.user.entity.RPanUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rocket.pan.server.modules.user.vo.UserInfoVO;

/**
 * @author 19750
 * @description 针对表【r_pan_user(用户信息表)】的数据库操作Service
 * @createDate 2023-11-11 14:36:21
 */

public interface IUserService extends IService<RPanUser> {

    /**
     * 用户注册业务
     *
     * @param userRegisterContext
     * @return
     */
    Long register(UserRegisterContext userRegisterContext);

    /**
     * 用户登录业务
     *
     * @param userLoginContext
     * @return
     */
    String login(UserLoginContext userLoginContext);

    /**
     * 用户退出登录
     *
     * @param userId
     */
    void exit(Long userId);

    /**
     * 用户忘记密码-校验用户名称
     *
     * @param checkUsernameContext
     * @return
     */
    String checkUsername(CheckUsernameContext checkUsernameContext);

    /**
     * 用户忘记密码-校验密保答案
     *
     * @param checkAnswerContext
     * @return
     */
    String checkAnswer(CheckAnswerContext checkAnswerContext);

    /**
     * 用户忘记密码-重置用户密码参数
     *
     * @param resetPasswordContext
     */
    void resetPassword(ResetPasswordContext resetPasswordContext);

    /**
     * 在线修改密码
     *
     * @param changePasswordContext
     */
    void changePassword(ChangePasswordContext changePasswordContext);

    /**
     * 查询在线用户的基本信息
     *
     * @param userId
     * @return
     */
    UserInfoVO info(Long userId);
}
