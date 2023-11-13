package com.rocket.pan.server.modules.user.converter;

import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.rocket.pan.server.modules.user.context.*;
import com.rocket.pan.server.modules.user.entity.RPanUser;
import com.rocket.pan.server.modules.user.po.*;
import com.rocket.pan.server.modules.user.vo.UserInfoVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 用户模块实体转换工具类
 */
@Mapper(componentModel = "spring")
public interface UserConverter {
    /**
     * UserRegisterPO转化成UserRegisterContext
     *
     * @param userRegisterPO UserRegisterPO
     * @return UserRegisterContext
     */
    UserRegisterContext userRegisterPO2UserRegisterContext(UserRegisterPO userRegisterPO);

    /**
     * UserRegisterContext转RPanUser
     *
     * @param userRegisterContext
     * @return
     */
    @Mapping(target = "password", ignore = true)
    RPanUser userRegisterContext2RPanUser(UserRegisterContext userRegisterContext);

    /**
     * UserLoginPO转UserLoginContext
     *
     * @param userLoginPO
     * @return
     */
    UserLoginContext userLoginPO2UserLoginContext(UserLoginPO userLoginPO);

    /**
     * CheckUsernamePO 转 CheckUsernameContext
     *
     * @param checkUsernamePO
     * @return
     */
    CheckUsernameContext checkUsernamePO2CheckUsernameContext(CheckUsernamePO checkUsernamePO);

    /**
     * CheckAnswerPO 转 CheckAnswerContext
     *
     * @param checkAnswerPO
     * @return
     */
    CheckAnswerContext checkAnswerPO2CheckAnswerContext(CheckAnswerPO checkAnswerPO);

    /**
     * ResetPasswordPO 转 ResetPasswordContext
     *
     * @param resetPasswordPO
     * @return
     */
    ResetPasswordContext resetPasswordPO2ResetPasswordContext(ResetPasswordPO resetPasswordPO);

    /**
     * ChangePasswordPO 转 ChangePasswordContext
     *
     * @param changePasswordPO
     * @return
     */
    ChangePasswordContext changePasswordPO2ChangePasswordContext(ChangePasswordPO changePasswordPO);

    /**
     * 拼装用户基本信息返回实体
     * 多个映射关系实体，需要自己手动指定
     *
     * @param rPanUser
     * @param rPanUserFile
     * @return
     */
    @Mapping(source = "rPanUser.username", target = "username")
    @Mapping(source = "rPanUserFile.fileId", target = "rootFileId")
    @Mapping(source = "rPanUserFile.filename", target = "rootFilename")
    UserInfoVO assembleUserInfoVO(RPanUser rPanUser, RPanUserFile rPanUserFile);
}
