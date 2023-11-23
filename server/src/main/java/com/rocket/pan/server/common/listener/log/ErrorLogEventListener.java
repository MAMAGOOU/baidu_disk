package com.rocket.pan.server.common.listener.log;

import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.server.common.event.log.ErrorLogEvent;
import com.rocket.pan.server.modules.log.entity.RPanErrorLog;
import com.rocket.pan.server.modules.log.service.IErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 系统错误日子监听器
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class ErrorLogEventListener {
    @Autowired
    private IErrorLogService iErrorLogService;

    /**
     * 监听系统错误日志事件，并保存在数据库中
     *
     * @param event
     */
    @EventListener(ErrorLogEvent.class)
    @Async
    public void saveErrorLog(ErrorLogEvent event) {
        RPanErrorLog record = new RPanErrorLog();
        record.setId(IdUtil.get());
        record.setLogContent(event.getErrorMsg());
        record.setLogStatus(0);
        record.setCreateUser(event.getUserId());
        record.setCreateTime(new Date());
        record.setUpdateUser(event.getUserId());
        record.setUpdateTime(new Date());

        iErrorLogService.save(record);
    }
}
