package com.rocket.pan.server.common.event.log;

import lombok.*;
import org.springframework.context.ApplicationEvent;

/**
 * @author 19750
 * @version 1.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ErrorLogEvent extends ApplicationEvent {
    /**
     * 错误日子的内容
     */
    private String errorMsg;

    /**
     * 当前登陆的用户ID
     */
    private Long userId;

    public ErrorLogEvent(Object source, String errorMsg, Long userId) {
        super(source);
        this.errorMsg = errorMsg;
        this.userId = userId;
    }
}
