package com.rocket.pan.server.modules.user.context;

import lombok.Data;

import java.io.Serializable;

/**
 * 校验密保答案PO对象
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class CheckAnswerContext implements Serializable {
    private static final long serialVersionUID = -947015711857341702L;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 密保问题
     */
    private String question;

    /**
     * 密保答案
     */
    private String answer;
}
