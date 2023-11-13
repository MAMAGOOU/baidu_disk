package com.rocket.pan.server.modules.user.controller;

import com.rocket.pan.response.R;
import com.rocket.pan.server.common.annotation.LoginIgnore;
import com.rocket.pan.server.common.utils.UserIdUtil;
import com.rocket.pan.server.modules.user.context.*;
import com.rocket.pan.server.modules.user.converter.UserConverter;
import com.rocket.pan.server.modules.user.po.*;
import com.rocket.pan.server.modules.user.service.IUserService;
import com.rocket.pan.server.modules.user.vo.UserInfoVO;
import com.rocket.pan.util.IdUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户模块的控制器实体类
 *
 * @author 19750
 * @version 1.0
 */
@Api(tags = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private UserConverter userConverter;


    @ApiOperation(
            value = "用户注册接口",
            notes = "该接口实现了用户注册接口，实现了幂等性逻辑，在并发环境下可以放心调用",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @LoginIgnore
    @PostMapping("/register")
    public R register(@Validated @RequestBody UserRegisterPO userRegisterPO) {
        UserRegisterContext userRegisterContext = userConverter.userRegisterPO2UserRegisterContext(userRegisterPO);
        Long userId = iUserService.register(userRegisterContext);
        return R.data(IdUtil.encrypt(userId));
    }

    @LoginIgnore
    @ApiOperation(
            value = "用户登录接口",
            notes = "该接口提供了用户登录的功能，成功登陆之后，会返回有时效性的accessToken供后续服务使用",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("login")
    public R login(@Validated @RequestBody UserLoginPO userLoginPO) {
        UserLoginContext userLoginContext = userConverter.userLoginPO2UserLoginContext(userLoginPO);
        String accessToken = iUserService.login(userLoginContext);
        return R.data(accessToken);
    }

    @ApiOperation(
            value = "用户登出接口",
            notes = "该接口提供了用户登出的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("exit")
    public R exit() {
        iUserService.exit(UserIdUtil.get());
        return R.success();
    }

    @ApiOperation(
            value = "用户忘记密码-校验用户名",
            notes = "该接口提供了用户忘记密码-校验用户名的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @LoginIgnore
    @PostMapping("username/check")
    public R checkUsername(@Validated @RequestBody CheckUsernamePO checkUsernamePO) {
        CheckUsernameContext checkUsernameContext = userConverter.checkUsernamePO2CheckUsernameContext(checkUsernamePO);
        String question = iUserService.checkUsername(checkUsernameContext);
        // 将该用户下设置密保问题返回给前端
        return R.data(question);
    }

    @ApiOperation(
            value = "用户忘记密码-校验密保答案",
            notes = "该接口提供了用户忘记密码-校验密保答案的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @LoginIgnore
    @PostMapping("answer/check")
    public R checkAnswer(@Validated @RequestBody CheckAnswerPO checkAnswerPO) {
        CheckAnswerContext checkAnswerContext = userConverter.checkAnswerPO2CheckAnswerContext(checkAnswerPO);
        String token = iUserService.checkAnswer(checkAnswerContext);
        return R.data(token);
    }

    @ApiOperation(
            value = "用户忘记密码-重置新密码",
            notes = "该接口提供了用户忘记密码-重置新密码的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("password/reset")
    @LoginIgnore
    public R resetPassword(@Validated @RequestBody ResetPasswordPO resetPasswordPO) {
        ResetPasswordContext resetPasswordContext = userConverter.resetPasswordPO2ResetPasswordContext(resetPasswordPO);
        iUserService.resetPassword(resetPasswordContext);
        return R.success();
    }

    @ApiOperation(
            value = "用户在线修改密码",
            notes = "该接口提供了用户在线修改密码的功能",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @PostMapping("password/change")
    public R changePassword(@Validated @RequestBody ChangePasswordPO changePasswordPO) {
        ChangePasswordContext changePasswordContext = userConverter.changePasswordPO2ChangePasswordContext(changePasswordPO);
        changePasswordContext.setUserId(UserIdUtil.get());
        iUserService.changePassword(changePasswordContext);
        return R.success();
    }

    @ApiOperation(
            value = "查询登录用户的基本信息",
            notes = "该接口提供了查询登录用户的基本信息的功能",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @GetMapping("/")
    public R<UserInfoVO> info() {
        UserInfoVO userInfoVO = iUserService.info(UserIdUtil.get());
        return R.data(userInfoVO);
    }
}
