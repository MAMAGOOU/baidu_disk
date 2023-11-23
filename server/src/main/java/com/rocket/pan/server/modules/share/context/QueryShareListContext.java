package com.rocket.pan.server.modules.share.context;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询用户已有的分享链接列表的上下文实体对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class QueryShareListContext implements Serializable {
    private static final long serialVersionUID = 4348516200563466548L;

    /**
     * 当前登录的用户ID
     */
    private Long userId;
}
