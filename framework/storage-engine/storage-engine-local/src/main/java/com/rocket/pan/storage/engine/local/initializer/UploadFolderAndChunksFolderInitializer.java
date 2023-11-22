package com.rocket.pan.storage.engine.local.initializer;

import com.rocket.pan.storage.engine.local.config.LocalStorageEngineConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 初始化上传文件根目录和文件分片存储根目录的初始化器
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
public class UploadFolderAndChunksFolderInitializer implements CommandLineRunner {

    @Autowired
    private LocalStorageEngineConfig config;

    @Override
    public void run(String... args) throws Exception {
        FileUtils.forceMkdir(new File(config.getRootFilePath()));
        log.info("the root file path has been created!");
        FileUtils.forceMkdir(new File(config.getRootFileChunkPath()));
        log.info("the root file chunk path has been created!");
    }
}
