package com.rocket.pan.server.modules.file.service;

import com.rocket.pan.server.modules.file.context.FileChunkMergeAndSaveContext;
import com.rocket.pan.server.modules.file.context.FileChunkSaveContext;
import com.rocket.pan.server.modules.file.context.FileSaveContext;
import com.rocket.pan.server.modules.file.entity.RPanFile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 19750
 * @description 针对表【r_pan_file(物理文件信息表)】的数据库操作Service
 * @createDate 2023-11-11 14:40:41
 */
public interface IFileService extends IService<RPanFile> {
    /**
     * 上传单文件并保存实体记录
     *
     * @param context
     */
    void saveFile(FileSaveContext context);


    /**
     * 合并分片物理文件并保存文件记录
     *
     * @param fileChunkMergeAndSaveContext
     */
    void mergeFileChunkAndSaveFile(FileChunkMergeAndSaveContext fileChunkMergeAndSaveContext);
}
