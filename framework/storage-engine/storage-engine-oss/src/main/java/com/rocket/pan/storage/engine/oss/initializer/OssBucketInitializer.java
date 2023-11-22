package com.rocket.pan.storage.engine.oss.initializer;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.Bucket;
import com.rocket.pan.core.exception.RPanFrameworkException;
import com.rocket.pan.storage.engine.oss.OSSStorageEngine;
import com.rocket.pan.storage.engine.oss.config.OssStorageEngineConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
public class OssBucketInitializer implements CommandLineRunner {

    @Autowired
    private OssStorageEngineConfig config;

    @Autowired
    private OSSClient client;

    @Override
    public void run(String... args) throws Exception {
        String bucketName = config.getBucketName();
        List<Bucket> buckets = client.listBuckets();
        List<String> bucketNames = buckets.stream().map(bucket -> bucket.getName()).collect(Collectors.toList());

        boolean bucketExist = bucketNames.contains(bucketName);

        if (!bucketExist && config.getAutoCreateBucket()) {
            client.createBucket(bucketName);
        }

        if (!bucketExist && !config.getAutoCreateBucket()) {
            throw new RPanFrameworkException("the bucket " + bucketName + " is not available");
        }

        log.info("the bucket " + bucketName + " have been created!");
    }
}
