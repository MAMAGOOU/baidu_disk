package com.rocket.pan.server.modules.file.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.core.exception.RPanBusinessException;
import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.lock.core.annotation.Lock;
import com.rocket.pan.server.common.config.PanServerConfig;
import com.rocket.pan.server.modules.file.context.FileChunkMergeAndSaveContext;
import com.rocket.pan.server.modules.file.context.FileChunkSaveContext;
import com.rocket.pan.server.modules.file.converter.FileConverter;
import com.rocket.pan.server.modules.file.entity.RPanFileChunk;
import com.rocket.pan.server.modules.file.enums.MergeFlagEnum;
import com.rocket.pan.server.modules.file.service.IFileChunkService;
import com.rocket.pan.server.modules.file.mapper.RPanFileChunkMapper;
import com.rocket.pan.storage.engine.core.StorageEngine;
import com.rocket.pan.storage.engine.core.context.StoreFileChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;

/**
 * @author 19750
 * @description 针对表【r_pan_file_chunk(文件分片信息表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:40:41
 */
@Service
public class FileChunkServiceImpl extends ServiceImpl<RPanFileChunkMapper, RPanFileChunk>
        implements IFileChunkService {

    @Autowired
    private PanServerConfig config;
    @Autowired
    private FileConverter fileConverter;

    @Autowired
    private StorageEngine storageEngine;

    /**
     * 文件分片保存
     * 1. 保存文件分片和记录
     * 2. 判断文件分片是否已经全部上传完成
     *
     * @param context
     */
    @Override
    @Lock(name = "ossSaveChunkFile", keys = {"#context.userId", "#context.identifier"}, expireSecond = 10)
    public void saveChunkFile(FileChunkSaveContext context) {
        doSaveChunkFile(context);
        doJudgeMergeFile(context);
    }


    /**
     * 判断是否所有的分片都已经上传完成
     *
     * @param context
     */
    private void doJudgeMergeFile(FileChunkSaveContext context) {
        LambdaQueryWrapper<RPanFileChunk> lambdaQueryWrapper = Wrappers.<RPanFileChunk>lambdaQuery()
                .eq(RPanFileChunk::getIdentifier, context.getIdentifier())
                .eq(RPanFileChunk::getCreateUser, context.getUserId());
        int count = count(lambdaQueryWrapper);
        if (count == context.getTotalChunks()) {
            context.setMergeFlagEnum(MergeFlagEnum.READY);

        }
    }

    /**
     * 执行文件分片上传保存的操作
     * 1. 委托文件存储引擎存储文件分片
     * 2. 保存文件分片记录
     *
     * @param context
     */
    private void doSaveChunkFile(FileChunkSaveContext context) {
        doStoreFileChunk(context);
        doSaveRecord(context);
    }

    /**
     * 保存文件分片记录
     *
     * @param context
     */
    private void doSaveRecord(FileChunkSaveContext context) {
        RPanFileChunk record = new RPanFileChunk();
        record.setId(IdUtil.get());
        record.setIdentifier(context.getIdentifier());
        record.setRealPath(context.getRealPath());
        record.setChunkNumber(context.getChunkNumber());
        record.setExpirationTime(DateUtil.offsetDay(new Date(), config.getChunkFileExpirationDays()));
        record.setCreateUser(context.getUserId());
        record.setCreateTime(new Date());

        if (!save(record)) {
            throw new RPanBusinessException("文件分片上传失败");
        }
    }

    /**
     * 委托文件存储引擎存储文件分片
     *
     * @param context
     */
    private void doStoreFileChunk(FileChunkSaveContext context) {
        try {
            StoreFileChunkContext storeFileChunkContext = fileConverter.fileChunkSaveContext2StoreFileChunkContext(context);
            storeFileChunkContext.setInputStream(context.getFile().getInputStream());
            storageEngine.storeChunk(storeFileChunkContext);
            context.setRealPath(storeFileChunkContext.getRealPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件分片上传失败");
        }
    }
}




