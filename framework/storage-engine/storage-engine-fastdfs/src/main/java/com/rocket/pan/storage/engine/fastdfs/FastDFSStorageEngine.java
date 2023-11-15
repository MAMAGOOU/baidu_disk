package com.rocket.pan.storage.engine.fastdfs;

import com.rocket.pan.storage.engine.core.AbstractStorageEngine;
import com.rocket.pan.storage.engine.core.context.DeleteFileContext;
import com.rocket.pan.storage.engine.core.context.MergeFileContext;
import com.rocket.pan.storage.engine.core.context.StoreFileChunkContext;
import com.rocket.pan.storage.engine.core.context.StoreFileContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 基于FastDFS实现的文件存储引擎
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class FastDFSStorageEngine extends AbstractStorageEngine {
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
    protected void doMergeFile(MergeFileContext context) throws IOException{

    }


}
