package com.rocket.pan.server.modules.file.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.rocket.pan.core.exception.RPanBusinessException;
import com.rocket.pan.core.utils.FileUtils;
import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.server.common.event.log.ErrorLogEvent;
import com.rocket.pan.server.modules.file.context.FileChunkMergeAndSaveContext;
import com.rocket.pan.server.modules.file.context.FileChunkSaveContext;
import com.rocket.pan.server.modules.file.context.FileSaveContext;
import com.rocket.pan.server.modules.file.entity.RPanFile;
import com.rocket.pan.server.modules.file.entity.RPanFileChunk;
import com.rocket.pan.server.modules.file.service.IFileChunkService;
import com.rocket.pan.server.modules.file.service.IFileService;
import com.rocket.pan.server.modules.file.mapper.RPanFileMapper;
import com.rocket.pan.storage.engine.core.StorageEngine;
import com.rocket.pan.storage.engine.core.context.DeleteFileContext;
import com.rocket.pan.storage.engine.core.context.MergeFileContext;
import com.rocket.pan.storage.engine.core.context.StoreFileContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 19750
 * @description 针对表【r_pan_file(物理文件信息表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:40:41
 */
@Service
public class FileServiceImpl extends ServiceImpl<RPanFileMapper, RPanFile>
        implements IFileService, ApplicationContextAware {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IFileChunkService iFileChunkService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    private StorageEngine storageEngine;

    /**
     * 上传单文件并保存实体记录
     * 1. 上传单文件
     * 2. 保存实体记录
     *
     * @param context
     */
    @Override
    public void saveFile(FileSaveContext context) {
        storeMultipartFile(context);
        RPanFile record = doSaveFile(context.getFilename(),
                context.getRealPath(),
                context.getTotalSize(),
                context.getIdentifier(),
                context.getUserId());
        context.setRecord(record);
    }


    /**
     * 合并分片物理文件并保存文件记录
     * 1. 委托文件存储引擎合并文件分片
     * 2. 保存物理文件记录
     *
     * @param context
     */
    @Override
    public void mergeFileChunkAndSaveFile(FileChunkMergeAndSaveContext context) {
        doMergeFileChunk(context);
        RPanFile record = doSaveFile(context.getFilename(), context.getRealPath(), context.getTotalSize(), context.getIdentifier(), context.getUserId());
        context.setRecord(record);
    }


    //  =======================================================private=============================================

    /**
     * 委托文件存储引擎合并文件分片
     * 1. 查询文件分片的记录
     * 2. 根据文件的分片记录去合并物理文件
     * 3. 删除文件分片记录
     * 4. 封装合并文件的真实存储路径到上下文信息中
     *
     * @param context
     */
    private void doMergeFileChunk(FileChunkMergeAndSaveContext context) {
        LambdaQueryWrapper<RPanFileChunk> lambdaQueryWrapper = Wrappers.<RPanFileChunk>lambdaQuery()
                .eq(RPanFileChunk::getIdentifier, context.getIdentifier())
                .eq(RPanFileChunk::getCreateUser, context.getUserId())
                .ge(RPanFileChunk::getExpirationTime, new Date());
        List<RPanFileChunk> chunkRecordList = iFileChunkService.list(lambdaQueryWrapper);

        if (ObjectUtil.isNull(chunkRecordList)) {
            throw new RPanBusinessException("该文件没有找到分片记录");
        }

        // 如果不为空，那么需要转化为一个真实路径的集合，进行分片下标排序，这样文件存储引擎就不需要关系排序的问题
        List<String> realPathList = chunkRecordList.stream()
                .sorted(Comparator.comparing(RPanFileChunk::getChunkNumber))
                .map(RPanFileChunk::getRealPath)
                .collect(Collectors.toList());

        try {
            MergeFileContext mergeFileContext = new MergeFileContext();
            mergeFileContext.setUserId(context.getUserId());
            mergeFileContext.setFilename(context.getFilename());
            mergeFileContext.setIdentifier(context.getIdentifier());
            mergeFileContext.setRealPathList(realPathList);

            storageEngine.mergeFile(mergeFileContext);

            context.setRealPath(mergeFileContext.getRealPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件分片合并失败");
        }

        List<Long> fileChunkRecordList = chunkRecordList.stream().map(RPanFileChunk::getId).collect(Collectors.toList());
        iFileChunkService.removeByIds(fileChunkRecordList);


    }

    /**
     * 保存实体文件记录
     *
     * @param filename
     * @param realPath
     * @param totalSize
     * @param identifier
     * @param userId
     * @return RPanFile
     */
    private RPanFile doSaveFile(String filename, String realPath, Long totalSize, String identifier, Long userId) {
        RPanFile record = aseembleRPanFile(filename, realPath, totalSize, identifier, userId);
        if (!save(record)) {

            try {
                // 实体文件记录保存失败，需要删除到已经上传的物理文件
                DeleteFileContext deleteFileContext = new DeleteFileContext();
                deleteFileContext.setRealFilePathList(Lists.newArrayList(realPath));
                storageEngine.delete(deleteFileContext);
            } catch (IOException e) {
                // 如果发生了不可控的异常，比如机器出现故障，那么此时需要人工来进行处理
                e.printStackTrace();
                // 发布事件
                ErrorLogEvent errorLogEvent = new ErrorLogEvent(this, "文件物理删除失败，请执行手动删除！文件路径为：" + realPath, userId);
                applicationContext.publishEvent(errorLogEvent);
            }
        }
        return record;
    }

    /**
     * 拼装文件实体对象
     *
     * @param filename
     * @param realPath
     * @param totalSize
     * @param identifier
     * @param userId
     * @return
     */
    private RPanFile aseembleRPanFile(String filename, String realPath, Long totalSize, String identifier, Long userId) {
        RPanFile record = new RPanFile();

        record.setFileId(IdUtil.get());
        record.setFilename(filename);
        record.setRealPath(realPath);
        record.setFileSize(String.valueOf(totalSize));
        record.setFileSizeDesc(FileUtils.byteCountToDisplaySize(totalSize));
        record.setFileSuffix(FileUtils.getFileSuffix(filename));
        record.setFilePreviewContentType(FileUtils.getContentType(realPath));
        record.setIdentifier(identifier);
        record.setCreateUser(userId);
        record.setCreateTime(new Date());

        return record;
    }

    /**
     * 上传单文件，该方法委托文件存储引擎进行实现
     *
     * @param context
     */
    private void storeMultipartFile(FileSaveContext context) {

        try {
            StoreFileContext storeFileContext = new StoreFileContext();
            storeFileContext.setFilename(context.getFilename());
            storeFileContext.setInputStream(context.getFile().getInputStream());
            storeFileContext.setTotalSize(context.getTotalSize());

            storageEngine.store(storeFileContext);

            context.setRealPath(storeFileContext.getRealPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件上传失败");
        }
    }


}




