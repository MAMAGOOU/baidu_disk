package com.rocket.pan.server.modules.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.server.modules.file.entity.RPanFile;
import com.rocket.pan.server.modules.file.service.RPanFileService;
import com.rocket.pan.server.modules.file.mapper.RPanFileMapper;
import org.springframework.stereotype.Service;

/**
* @author 19750
* @description 针对表【r_pan_file(物理文件信息表)】的数据库操作Service实现
* @createDate 2023-11-11 14:40:41
*/
@Service
public class RPanFileServiceImpl extends ServiceImpl<RPanFileMapper, RPanFile>
    implements RPanFileService{

}




