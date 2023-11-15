package com.rocket.pan.server.common.event.file;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;

import java.util.List;

/**
 * 文件删除事件
 *
 * @author 19750
 * @version 1.0
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DeleteFileEvent extends ApplicationEvent {
    private List<Long> fileIdList;

    public DeleteFileEvent(Object source, List<Long> fileIdList) {
        super(source);
        this.fileIdList = fileIdList;
    }
}
