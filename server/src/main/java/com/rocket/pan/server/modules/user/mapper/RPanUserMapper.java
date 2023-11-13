package com.rocket.pan.server.modules.user.mapper;

import com.rocket.pan.server.modules.user.entity.RPanUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author 19750
 * @description 针对表【r_pan_user(用户信息表)】的数据库操作Mapper
 * @createDate 2023-11-11 14:36:21
 * @Entity com.rocket.pan.server.modules.user.entity.RPanUser
 */
public interface RPanUserMapper extends BaseMapper<RPanUser> {

    /**
     * 通过用户名称查询用户设置的密保问题
     *
     * @param username
     * @return
     */
    String selectQuestionByUsername(@Param("username") String username);
}




