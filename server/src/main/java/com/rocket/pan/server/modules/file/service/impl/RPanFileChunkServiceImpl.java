package com.rocket.pan.server.modules.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.server.modules.file.entity.RPanFileChunk;
import com.rocket.pan.server.modules.file.service.RPanFileChunkService;
import com.rocket.pan.server.modules.file.mapper.RPanFileChunkMapper;
import org.springframework.stereotype.Service;

/**
* @author 19750
* @description 针对表【r_pan_file_chunk(文件分片信息表)】的数据库操作Service实现
* @createDate 2023-11-11 14:40:41
*/
@Service
public class RPanFileChunkServiceImpl extends ServiceImpl<RPanFileChunkMapper, RPanFileChunk>
    implements RPanFileChunkService{

}




