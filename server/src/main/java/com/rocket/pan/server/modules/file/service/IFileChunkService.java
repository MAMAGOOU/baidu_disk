package com.rocket.pan.server.modules.file.service;

import com.rocket.pan.server.modules.file.context.FileChunkMergeAndSaveContext;
import com.rocket.pan.server.modules.file.context.FileChunkSaveContext;
import com.rocket.pan.server.modules.file.entity.RPanFileChunk;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 19750
 * @description 针对表【r_pan_file_chunk(文件分片信息表)】的数据库操作Service
 * @createDate 2023-11-11 14:40:41
 */
public interface IFileChunkService extends IService<RPanFileChunk> {

    /**
     * 文件分片保存
     *
     * @param fileChunkSaveContext
     */
    void saveChunkFile(FileChunkSaveContext fileChunkSaveContext);



}
