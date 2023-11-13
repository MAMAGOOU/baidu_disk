package com.rocket.pan.server.modules.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.server.modules.user.entity.RPanUserSearchHistory;
import com.rocket.pan.server.modules.user.service.IUserSearchHistoryService;
import com.rocket.pan.server.modules.user.mapper.RPanUserSearchHistoryMapper;
import org.springframework.stereotype.Service;

/**
 * @author 19750
 * @description 针对表【r_pan_user_search_history(用户搜索历史表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:36:21
 */
@Service("userSearchHistoryService")
public class UserSearchHistoryServiceImpl extends ServiceImpl<RPanUserSearchHistoryMapper, RPanUserSearchHistory>
        implements IUserSearchHistoryService {

}




