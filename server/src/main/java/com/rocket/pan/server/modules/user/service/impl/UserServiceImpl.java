package com.rocket.pan.server.modules.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.cache.core.constants.CacheConstants;
import com.rocket.pan.exception.RPanBusinessException;
import com.rocket.pan.response.ResponseCode;
import com.rocket.pan.server.modules.file.constants.FileConstants;
import com.rocket.pan.server.modules.file.context.CreateFolderContext;
import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.rocket.pan.server.modules.file.service.IUserFileService;
import com.rocket.pan.server.modules.user.constants.UserConstants;
import com.rocket.pan.server.modules.user.context.*;
import com.rocket.pan.server.modules.user.converter.UserConverter;
import com.rocket.pan.server.modules.user.entity.RPanUser;
import com.rocket.pan.server.modules.user.service.IUserService;
import com.rocket.pan.server.modules.user.mapper.RPanUserMapper;
import com.rocket.pan.server.modules.user.vo.UserInfoVO;
import com.rocket.pan.util.IdUtil;
import com.rocket.pan.util.JwtUtil;
import com.rocket.pan.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

/**
 * @author 19750
 * @description 针对表【r_pan_user(用户信息表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:36:21
 */
@Service("userService")
public class UserServiceImpl extends ServiceImpl<RPanUserMapper, RPanUser>
        implements IUserService {

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private IUserFileService iUserFileService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 用户注册业务
     * 1. 注册用户信息，保证幂等性，用户名全局唯一
     * 2. 创建新用户的根目录信息
     * <p>
     * 通过数据库对用户名字段添加唯一索引，我们上游业务捕获对应异常冲突，转化返回
     *
     * @param userRegisterContext
     * @return
     */
    @Override
    public Long register(UserRegisterContext userRegisterContext) {
        assembleUserEntity(userRegisterContext);
        doRegister(userRegisterContext);
        createUserRootFolder(userRegisterContext);
        return userRegisterContext.getEntity().getUserId();
    }

    /**
     * 用户登录业务
     * <p>
     * 1. 用户登录信息校验
     * 2. 具有时效性的accessToken
     * 3. 将accessToken缓存起来实现单机登录
     *
     * @param userLoginContext
     * @return
     */
    @Override
    public String login(UserLoginContext userLoginContext) {
        checkLoginInfo(userLoginContext);
        generateAndSaveAccessToken(userLoginContext);
        return userLoginContext.getAccessToken();
    }

    /**
     * 用户退出登录
     * 1. 清理到用户登录缓存即可
     *
     * @param userId
     */
    @Override
    public void exit(Long userId) {
        try {
            Cache cache = cacheManager.getCache(CacheConstants.R_PAN_CACHE_NAME);
            cache.evict(UserConstants.USER_LOGIN_PREFIX + userId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RPanBusinessException("用户退出登录失败");
        }
    }

    /**
     * 校验用户名称
     * 将传过来的用户名往数据库里查，如果能查到就说明用户名是有效的，如果不能查到就说明用户名没有效果
     *
     * @param checkUsernameContext
     * @return
     */
    @Override
    public String checkUsername(CheckUsernameContext checkUsernameContext) {
        String question = baseMapper.selectQuestionByUsername(checkUsernameContext.getUsername());
        if (StrUtil.isBlank(question)) {
            throw new RPanBusinessException("没有该用户");
        }
        return question;
    }

    /**
     * 用户忘记密码-校验密保答案
     *
     * @param checkAnswerContext
     * @return
     */
    @Override
    public String checkAnswer(CheckAnswerContext checkAnswerContext) {
        String username = checkAnswerContext.getUsername();
        String answer = checkAnswerContext.getAnswer();
        String question = checkAnswerContext.getQuestion();
        LambdaQueryWrapper<RPanUser> lambdaQueryWrapper = Wrappers.<RPanUser>lambdaQuery()
                .eq(StrUtil.isNotBlank(username), RPanUser::getUsername, username)
                .eq(StrUtil.isNotBlank(answer), RPanUser::getAnswer, answer)
                .eq(StrUtil.isNotBlank(question), RPanUser::getQuestion, question);
        int count = count(lambdaQueryWrapper);
        if (count == 0) {
            throw new RPanBusinessException("密码答案错误");
        }
        // 生成校验密保答案通过的临时token
        return generateCheckAnswerToken(checkAnswerContext);
    }

    /**
     * 用户忘记密码-重置用户密码参数
     * 1. 校验token是否有效
     * 2. 重置密码
     *
     * @param resetPasswordContext
     */
    @Override
    public void resetPassword(ResetPasswordContext resetPasswordContext) {
        checkForgetPasswordToken(resetPasswordContext);
        checkAndResetUserPassword(resetPasswordContext);
    }

    /**
     * 在线修改密码
     * 1. 校验旧密码
     * 2. 重置新密码
     * 3. 退出当前的登录状态
     *
     * @param changePasswordContext
     */
    @Override
    public void changePassword(ChangePasswordContext changePasswordContext) {
        checkOldPassword(changePasswordContext);
        doChangePassword(changePasswordContext);
        exitLoginStatus(changePasswordContext);
    }

    /**
     * 查询在线用户的基本信息
     * 1. 查询用户基本信息实体
     * 2. 查询用户根文件夹信息
     * 3. 拼装VO对象返回
     *
     * @param userId
     * @return
     */
    @Override
    public UserInfoVO info(Long userId) {
        RPanUser entity = getById(userId);
        if (ObjectUtil.isNull(entity)) {
            throw new RPanBusinessException("用户信息查询失败");
        }
        RPanUserFile rPanUserFile = getUserRootFileInfo(userId);
        if (ObjectUtil.isNull(rPanUserFile)) {
            throw new RPanBusinessException("查询用户文件夹信息失败");
        }

        return userConverter.assembleUserInfoVO(entity, rPanUserFile);
    }

    /**
     * 获取用户根文件夹信息实体
     *
     * @param userId
     * @return
     */
    private RPanUserFile getUserRootFileInfo(Long userId) {
        // 当前业务不可以写在user服务，需要委托为 UserFileService
        return iUserFileService.getUserRootFile(userId);
    }

    /**
     * 退出当前的登录状态
     *
     * @param changePasswordContext
     */
    private void exitLoginStatus(ChangePasswordContext changePasswordContext) {
        exit(changePasswordContext.getUserId());
    }

    /**
     * 修改新密码
     *
     * @param changePasswordContext
     */
    private void doChangePassword(ChangePasswordContext changePasswordContext) {
        String newPassword = changePasswordContext.getNewPassword();
        RPanUser entity = changePasswordContext.getEntity();
        String salt = entity.getSalt();

        String encNewPassword = PasswordUtil.encryptPassword(salt, newPassword);

        entity.setPassword(encNewPassword);

        if (!updateById(entity)) {
            throw new RPanBusinessException("修改用户密码失败");
        }
    }

    /**
     * 校验旧密码
     *
     * @param changePasswordContext
     */
    private void checkOldPassword(ChangePasswordContext changePasswordContext) {
        Long userId = changePasswordContext.getUserId();
        String oldPassword = changePasswordContext.getOldPassword();
        RPanUser entity = getById(userId);
        if (ObjectUtil.isNull(entity)) {
            throw new RPanBusinessException("用户信息不存在");
        }
        changePasswordContext.setEntity(entity);
        String encOldPassword = PasswordUtil.encryptPassword(entity.getSalt(), oldPassword);
        String dbOldPassword = entity.getPassword();
        if (ObjectUtil.notEqual(encOldPassword, dbOldPassword)) {
            throw new RPanBusinessException("旧密码不正确");
        }
    }

    /**
     * 校验用户信息并重置用户密码
     *
     * @param resetPasswordContext
     */
    private void checkAndResetUserPassword(ResetPasswordContext resetPasswordContext) {
        String username = resetPasswordContext.getUsername();
        String password = resetPasswordContext.getPassword();
        RPanUser entity = getRPanUserByUsername(username);
        if (ObjectUtil.isNull(entity)) {
            throw new RPanBusinessException("用户信息不存在");
        }
        String encryptPassword = PasswordUtil.encryptPassword(entity.getSalt(), password);
        entity.setPassword(encryptPassword);
        entity.setUpdateTime(new Date());

        if (!updateById(entity)) {
            throw new RPanBusinessException("重置用户密码失败");
        }
    }


    /**
     * 校验忘记密码的token是否有效
     *
     * @param resetPasswordContext
     */
    private void checkForgetPasswordToken(ResetPasswordContext resetPasswordContext) {
        String token = resetPasswordContext.getToken();
        Object value = JwtUtil.analyzeToken(token, UserConstants.FORGET_USERNAME);
        if (ObjectUtil.isNull(value)) {
            throw new RPanBusinessException(ResponseCode.TOKEN_EXPIRE);
        }
        String tokenUsername = String.valueOf(value);
        if (ObjectUtil.notEqual(tokenUsername, resetPasswordContext.getUsername())) {
            throw new RPanBusinessException("token错误");
        }
    }

    /**
     * 用户忘记密码-生成校验密保答案通过的临时token
     *
     * @param checkAnswerContext
     * @return
     */
    private String generateCheckAnswerToken(CheckAnswerContext checkAnswerContext) {
        String token = JwtUtil.generateToken(checkAnswerContext.getUsername(),
                UserConstants.FORGET_USERNAME,
                checkAnswerContext.getUsername(),
                UserConstants.FIVE_MINUTES_LONG);

        return token;
    }

    /**
     * 生成并保存登录后的凭证
     *
     * @param userLoginContext
     */
    private void generateAndSaveAccessToken(UserLoginContext userLoginContext) {
        RPanUser entity = userLoginContext.getEntity();
        String accessToken = JwtUtil.generateToken(entity.getUsername(),
                UserConstants.LOGIN_USER_ID,
                entity.getUserId(),
                UserConstants.ONE_DAY_LONG);
        Cache cache = cacheManager.getCache(CacheConstants.R_PAN_CACHE_NAME);
        cache.put(UserConstants.USER_LOGIN_PREFIX + entity.getUserId(), accessToken);
        userLoginContext.setAccessToken(accessToken);
    }

    /**
     * 校验用户名、密码
     *
     * @param userLoginContext
     */
    private void checkLoginInfo(UserLoginContext userLoginContext) {
        String username = userLoginContext.getUsername();
        String password = userLoginContext.getPassword();
        RPanUser entity = getRPanUserByUsername(username);
        if (ObjectUtil.isNull(entity)) {
            throw new RPanBusinessException("用户名称不存在");
        }
        String salt = entity.getSalt();
        String encryptPassword = PasswordUtil.encryptPassword(salt, password);
        String dbPassword = entity.getPassword();
        if (ObjectUtil.notEqual(encryptPassword, dbPassword)) {
            throw new RPanBusinessException("密码信息不正确");
        }

        userLoginContext.setEntity(entity);
    }

    /**
     * 用过用户名称获取用户实体信息
     *
     * @param username
     * @return
     */
    private RPanUser getRPanUserByUsername(String username) {
        LambdaQueryWrapper<RPanUser> lambdaQueryWrapper = Wrappers.<RPanUser>lambdaQuery()
                .eq(ObjectUtil.isNotEmpty(username), RPanUser::getUsername, username);
        return getOne(lambdaQueryWrapper);
    }

    /**
     * 创建用户根目录信息
     *
     * @param userRegisterContext
     */
    private void createUserRootFolder(UserRegisterContext userRegisterContext) {
        CreateFolderContext createFolderContext = new CreateFolderContext();
        createFolderContext.setParentId(FileConstants.TOP_PARENT_ID);
        createFolderContext.setUserId(userRegisterContext.getEntity().getUserId());
        createFolderContext.setFolderName(FileConstants.ALL_FILE_CN_STR);

        iUserFileService.createFolder(createFolderContext);
    }

    /**
     * 实现注册用户的业务
     * 需要捕获数据库的唯一索引冲突异常，来实现全局用户名称唯一
     *
     * @param userRegisterContext
     */
    private void doRegister(UserRegisterContext userRegisterContext) {
        RPanUser entity = userRegisterContext.getEntity();
        if (ObjectUtil.isNotNull(entity)) {
            try {
                if (!save(entity)) {
                    throw new RPanBusinessException("用户注册失败");
                }
            } catch (DuplicateKeyException duplicateKeyException) {
                throw new RPanBusinessException("用户名已经存在！");
            }
            return;
        }
        throw new RPanBusinessException(ResponseCode.ERROR);
    }

    /**
     * 实体转化
     * 将上下文信息转为用户实体，封装进入上下文
     *
     * @param userRegisterContext
     */
    private void assembleUserEntity(UserRegisterContext userRegisterContext) {
        RPanUser entity = userConverter.userRegisterContext2RPanUser(userRegisterContext);
        String salt = PasswordUtil.getSalt();
        String password = PasswordUtil.encryptPassword(salt, userRegisterContext.getPassword());
        entity.setSalt(salt);
        entity.setUserId(IdUtil.get());
        entity.setPassword(password);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());

        userRegisterContext.setEntity(entity);
    }
}




