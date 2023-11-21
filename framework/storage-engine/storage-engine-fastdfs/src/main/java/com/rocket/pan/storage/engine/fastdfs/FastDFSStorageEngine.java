package com.rocket.pan.storage.engine.fastdfs;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.rocket.pan.core.constants.RPanConstants;
import com.rocket.pan.core.exception.RPanFrameworkException;
import com.rocket.pan.core.utils.FileUtils;
import com.rocket.pan.storage.engine.core.AbstractStorageEngine;
import com.rocket.pan.storage.engine.core.context.*;
import com.rocket.pan.storage.engine.fastdfs.config.FastDFSStorageEngineConfig;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 基于FastDFS实现的文件存储引擎
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class FastDFSStorageEngine extends AbstractStorageEngine {

    @Autowired
    private FastFileStorageClient client;

    @Autowired
    private FastDFSStorageEngineConfig config;

    /**
     * 执行保存物理文件的动作
     * 下沉到具体的子类实现
     *
     * @param context
     */
    @Override
    protected void doStore(StoreFileContext context) throws IOException {
        StorePath storePath = client.uploadFile(config.getGroup(), context.getInputStream(),
                context.getTotalSize(), FileUtils.getFileExtName(context.getFilename()));
        context.setRealPath(storePath.getFullPath());
    }

    /**
     * 执行删除物理文件的动作
     * 下沉到子类进行实现
     *
     * @param context
     */
    @Override
    protected void doDelete(DeleteFileContext context) throws IOException {
        List<String> realFilePathList = context.getRealFilePathList();
        if (CollectionUtils.isNotEmpty(realFilePathList)) {
            realFilePathList.stream().forEach(client::deleteFile);
        }
    }

    /**
     * 执行文件分片，下沉到底层进行实现
     * FastDFS底层是不支持分片
     *
     * @param context
     */
    @Override
    protected void doStoreChunk(StoreFileChunkContext context) throws IOException {
        throw new RPanFrameworkException("FastDFS不支持分片上传的操作");
    }

    /**
     * 执行文件分片合并动作
     * 下沉到底层实现
     *
     * @param context
     */
    @Override
    protected void doMergeFile(MergeFileContext context) throws IOException {
        throw new RPanFrameworkException("FastDFS不支持分片上传的操作");
    }


    /**
     * 读取文件内容并写入到输出流中
     * 下沉到子类中实现
     */
    @Override
    protected void doReadFile(ReadFileContext context) throws IOException {
        String realPath = context.getRealPath();
        String group = realPath.substring(RPanConstants.ZERO_INT, realPath.indexOf(RPanConstants.SLASH_STR));
        String path = realPath.substring(realPath.indexOf(RPanConstants.SLASH_STR) + RPanConstants.ONE_INT);

        DownloadByteArray downloadByteArray = new DownloadByteArray();
        byte[] bytes = client.downloadFile(group, path, downloadByteArray);

        OutputStream outputStream = context.getOutputStream();
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }


}
