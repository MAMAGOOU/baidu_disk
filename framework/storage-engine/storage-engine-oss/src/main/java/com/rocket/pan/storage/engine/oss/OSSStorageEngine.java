package com.rocket.pan.storage.engine.oss;

import com.rocket.pan.storage.engine.core.AbstractStorageEngine;
import com.rocket.pan.storage.engine.core.context.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 基于OSS的文件存储引擎实现
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class OSSStorageEngine extends AbstractStorageEngine {
    /**
     * 执行保存物理文件的动作
     * 下沉到具体的子类实现
     *
     * @param context
     */
    @Override
    protected void doStore(StoreFileContext context) throws IOException {

    }

    /**
     * 执行删除物理文件的动作
     * 下沉到子类进行实现
     *
     * @param context
     */
    @Override
    protected void doDelete(DeleteFileContext context) throws IOException {

    }

    /**
     * 执行文件分片，下沉到底层进行实现
     *
     * @param context
     */
    @Override
    protected void doStoreChunk(StoreFileChunkContext context) throws IOException {

    }

    /**
     * 执行文件分片合并动作
     * 下沉到底层实现
     *
     * @param context
     */
    @Override
    protected void doMergeFile(MergeFileContext context) throws IOException {

    }

    /**
     * 读取文件内容并写入到输出流中
     * 下沉到子类中实现
     */
    @Override
    protected void doReadFile(ReadFileContext context) throws IOException{

    }


}
