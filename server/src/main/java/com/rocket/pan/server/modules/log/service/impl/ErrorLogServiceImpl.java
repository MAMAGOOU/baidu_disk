package com.rocket.pan.server.modules.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.server.modules.log.entity.RPanErrorLog;
import com.rocket.pan.server.modules.log.service.IErrorLogService;
import com.rocket.pan.server.modules.log.mapper.RPanErrorLogMapper;
import org.springframework.stereotype.Service;

/**
* @author 19750
* @description 针对表【r_pan_error_log(错误日志表)】的数据库操作Service实现
* @createDate 2023-11-11 14:41:49
*/
@Service
public class ErrorLogServiceImpl extends ServiceImpl<RPanErrorLogMapper, RPanErrorLog>
    implements IErrorLogService {

}




