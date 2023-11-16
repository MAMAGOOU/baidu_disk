package com.rocket.pan.storage.engine.local;

import com.rocket.pan.core.utils.FileUtils;
import com.rocket.pan.storage.engine.core.AbstractStorageEngine;
import com.rocket.pan.storage.engine.core.context.*;
import com.rocket.pan.storage.engine.local.config.LocalStorageEngineConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * 本地文件存储引擎实现类
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class LocalStorageEngine extends AbstractStorageEngine {

    @Autowired
    private LocalStorageEngineConfig config;

    /**
     * 执行保存物理文件的动作
     * 下沉到具体的子类实现
     *
     * @param context
     */
    @Override
    protected void doStore(StoreFileContext context) throws IOException {
        String basePath = config.getRootFilePath();
        String realPath = FileUtils.generateStoreFileRealPath(basePath, context.getFilename());
        FileUtils.writeStream2File(context.getInputStream(), new File(realPath), context.getTotalSize());
        context.setRealPath(realPath);
    }

    /**
     * 执行删除物理文件的动作
     * 下沉到子类进行实现
     *
     * @param context
     */
    @Override
    protected void doDelete(DeleteFileContext context) throws IOException {
        FileUtils.deleteFiles(context.getRealFilePathList());
    }

    /**
     * 执行文件分片，下沉到底层进行实现
     *
     * @param context
     */
    @Override
    protected void doStoreChunk(StoreFileChunkContext context) throws IOException {
        String basePath = config.getRootFileChunkPath();
        String realFilePath = FileUtils.generateStoreFileChunkRealPath(basePath, context.getIdentifier(), context.getChunkNumber());
        FileUtils.writeStream2File(context.getInputStream(), new File(realFilePath), context.getTotalSize());
        context.setRealPath(realFilePath);
    }

    /**
     * 执行文件分片合并动作
     * 下沉到底层实现
     * 和单文件保存差不多，不过这里采取追加写
     *
     * @param context
     */
    @Override
    protected void doMergeFile(MergeFileContext context) throws IOException {
        String basePath = config.getRootFilePath();
        String realFilePath = FileUtils.generateStoreFileRealPath(basePath, context.getFilename());
        FileUtils.createFile(new File(realFilePath));
        List<String> chunkPaths = context.getRealPathList();
        for (String chunkPath : chunkPaths) {
            FileUtils.appendWrite(Paths.get(realFilePath), new File(chunkPath).toPath());
        }
        FileUtils.deleteFiles(chunkPaths);
        context.setRealPath(realFilePath);
    }

    /**
     * 读取文件内容并写入到输出流中
     * 下沉到子类中实现
     */
    @Override
    protected void doReadFile(ReadFileContext context) throws IOException {
        File file = new File(context.getRealPath());
        FileUtils.writeFile2OutputStream(new FileInputStream(file),context.getOutputStream(),file.length());
    }


}
