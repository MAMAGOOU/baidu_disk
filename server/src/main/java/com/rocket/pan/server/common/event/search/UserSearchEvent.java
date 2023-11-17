package com.rocket.pan.server.common.event.search;

import lombok.*;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;

/**
 * 用户搜索事件
 *
 * @author 19750
 * @version 1.0
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UserSearchEvent extends ApplicationEvent {
    private static final long serialVersionUID = 4307512337571187139L;
    private String keyword;
    private Long userId;


    public UserSearchEvent(Object source, String keyword, Long userId) {
        super(source);
        this.keyword = keyword;
        this.userId = userId;
    }
}
