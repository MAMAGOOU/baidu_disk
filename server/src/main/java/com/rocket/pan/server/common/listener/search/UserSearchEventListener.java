package com.rocket.pan.server.common.listener.search;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.server.common.event.search.UserSearchEvent;
import com.rocket.pan.server.modules.user.entity.RPanUserSearchHistory;
import com.rocket.pan.server.modules.user.service.IUserSearchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 用户搜索事件监听器
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class UserSearchEventListener {

    @Autowired
    private IUserSearchHistoryService iUserSearchHistoryService;

    /**
     * 监听用户搜索事件，将其保存到用户的搜索历史记录中
     *
     * @param event
     */
    @EventListener(classes = UserSearchEvent.class)
    public void saveSearchHistory(UserSearchEvent event) {
        RPanUserSearchHistory record = new RPanUserSearchHistory();

        record.setId(IdUtil.get());
        record.setUserId(event.getUserId());
        record.setSearchContent(event.getKeyword());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());

        try {
            iUserSearchHistoryService.save(record);
        } catch (DuplicateKeyException e) {
            LambdaUpdateWrapper<RPanUserSearchHistory> lambdaUpdateWrapper = Wrappers.<RPanUserSearchHistory>lambdaUpdate()
                    .eq(RPanUserSearchHistory::getUserId, event.getUserId())
                    .eq(RPanUserSearchHistory::getSearchContent, event.getKeyword())
                    .set(RPanUserSearchHistory::getUpdateTime, new Date());

            iUserSearchHistoryService.update(lambdaUpdateWrapper);
        }
    }
}
