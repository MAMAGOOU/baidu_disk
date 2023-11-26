package com.rocket.pan.storage.engine.oss;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.rocket.pan.core.constants.RPanConstants;
import com.rocket.pan.core.exception.RPanFrameworkException;
import com.rocket.pan.core.utils.FileUtils;
import com.rocket.pan.core.utils.UUIDUtil;
import com.rocket.pan.lock.core.annotation.Lock;
import com.rocket.pan.storage.engine.core.AbstractStorageEngine;
import com.rocket.pan.storage.engine.core.context.*;
import com.rocket.pan.storage.engine.oss.config.OssStorageEngineConfig;
import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于OSS的文件存储引擎实现
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class OSSStorageEngine extends AbstractStorageEngine {
    private static final Integer TEN_THOUSAND_INT = 10000;

    private static final String CACHE_KEY_TEMPLATE = "oss_cache_upload_id_%s_%s";

    private static final String IDENTIFIER_KEY = "identifier";

    private static final String UPLOAD_ID_KEY = "uploadId";

    private static final String USER_ID_KEY = "userId";

    private static final String PART_NUMBER_KEY = "partNumber";

    private static final String E_TAG_KEY = "eTag";

    private static final String PART_SIZE_KEY = "partSize";

    private static final String PART_CRC_KEY = "partCRC";
    @Autowired
    private OssStorageEngineConfig config;

    @Autowired
    private OSSClient client;

    /**
     * 执行保存物理文件的动作
     * 下沉到具体的子类实现
     *
     * @param context
     */
    @Override
    protected void doStore(StoreFileContext context) throws IOException {
        String realPath = getFilePath(FileUtils.getFileSuffix(context.getFilename()));
        client.putObject(config.getBucketName(), realPath, context.getInputStream());
        context.setRealPath(realPath);
    }

    /**
     * 获取对象的完整名称
     * 年/月/日/UUID.fileSuffix
     *
     * @param fileSuffix
     * @return
     */
    private String getFilePath(String fileSuffix) {
        return new StringBuffer()
                .append(DateUtil.thisYear())
                .append(RPanConstants.SLASH_STR)
                .append(DateUtil.thisMonth() + 1)
                .append(RPanConstants.SLASH_STR)
                .append(DateUtil.thisDayOfMonth())
                .append(RPanConstants.SLASH_STR)
                .append(UUIDUtil.getUUID())
                .append(fileSuffix)
                .toString();
    }

    /**
     * 执行删除物理文件的动作
     * 下沉到子类进行实现
     * <p>
     * 1. 获取所有需要删除的文件的存储路径
     * 2. 如果该存储路径是一个文件分片的路径，那么截取出对应的Object的name，然后取消文件分片操作
     * 3. 如果是一个正常的文件存储路径，直接执行物理删除即可
     *
     * @param context
     */
    @Override
    protected void doDelete(DeleteFileContext context) throws IOException {
        List<String> realFilePathList = context.getRealFilePathList();
        realFilePathList.stream().forEach(realPath -> {

            // 文件分片的存储路径
            if (checkHaveParams(realPath)) {
                JSONObject params = analysisUrlParams(realPath);
                if (Objects.nonNull(params) && !params.isEmpty()) {
                    String uploadId = params.getString(UPLOAD_ID_KEY);
                    String identifier = params.getString(IDENTIFIER_KEY);
                    Long userId = params.getLong(USER_ID_KEY);
                    String cacheKey = getCacheKey(identifier, userId);

                    getCache().evict(cacheKey);

                    try {
                        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(config.getBucketName(),
                                getBaseUrl(realPath), uploadId);
                        client.abortMultipartUpload(request);
                    } catch (Exception e) {

                    }
                }
            }
            // 普通文件的物理删除
            else {
                client.deleteObject(config.getBucketName(), realPath);
            }

        });
    }

    /**
     * 执行文件分片，下沉到底层进行实现
     * OSS文件分片上传步骤
     * 1. 初始化文件分片上传，初始化一个全局唯一的uploadId
     * 2. 并发上传文件分片，每个文件分片都需要带有初始化返回的uploadId
     * 3. 如果所有分片上传完成，触发文件分片合并操作
     * <p>
     * 1. 我们分片上传是在多线程环境执行的，我们的程序需要保证我们的初始化分片上传的操作只有一个线程可以做
     * 2. 我们所有文件分片都需要带有一个全局唯一的uploadId，该uploadId就需要放到一个线程的共享空间中
     * 3. 我们需要保证每个文件分片都可以单独调用文件分片上传，而不是依赖全局的uploadId（存在文件只上传一半）
     * <p>
     * 解决方案：
     * 1. 加锁，目前先按照单体架构，使用JVM的锁去保证一个线程初始化文件上传，后续扩展考虑redis分布式锁
     * 2. 使用缓存，缓存分为本地缓存以及分布式缓存，当前是单体架构可以考虑使用本地缓存，
     * 后期项目分布式架构升级，也需要升级缓存为分布式缓存，避免麻烦，我们直接支持分布式缓存
     * 3. 我们想把每个文件的key都能通过文件URL来获取，就需要定义一种数据格式，支持我们添加附件数据
     * 并且可以很方便的解析出来，我们的实现方案可以参考网络请求的URL格式：domain:port/url?paramKey=paramValue
     * <p>
     * 具体实现逻辑：
     * 1、校验文件分片数不得大于10000
     * 2、获取缓存key
     * 3、通过缓存key获取初始化后的实体对象，获取全局的uploadId和ObjectName
     * 4、如果获取为空，直接初始化
     * 5、执行文件分片上传的操作
     * 6、上传完成后，将全局的参数封装成一个可识别的url，保存在上下文里面，用于业务的落库操作
     *
     * @param context
     */
    @Override
    @Lock(name = "ossDoStoreChunk", keys = {"#context.userId", "#context.identifier"}, expireSecond = 10)
    protected void doStoreChunk(StoreFileChunkContext context) throws IOException {
        if (context.getTotalChunks() > TEN_THOUSAND_INT) {
            throw new RPanFrameworkException("分片数超过了限制，分片数不得大于： " + TEN_THOUSAND_INT);
        }

        String cacheKey = getCacheKey(context.getIdentifier(), context.getUserId());

        ChunkUploadEntity entity = getCache().get(cacheKey, ChunkUploadEntity.class);

        if (Objects.isNull(entity)) {
            entity = initChunkUpload(context.getFilename(), cacheKey);
        }

        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(config.getBucketName());
        request.setKey(entity.getObjectKey());
        request.setUploadId(entity.getUploadId());
        request.setInputStream(context.getInputStream());
        request.setPartSize(context.getCurrentChunkSize());
        request.setPartNumber(context.getChunkNumber());

        UploadPartResult result = client.uploadPart(request);

        if (Objects.isNull(result)) {
            throw new RPanFrameworkException("文件分片上传失败");
        }

        PartETag partETag = result.getPartETag();

        // 拼装文件分片的url
        JSONObject params = new JSONObject();
        params.put(IDENTIFIER_KEY, context.getIdentifier());
        params.put(UPLOAD_ID_KEY, entity.getUploadId());
        params.put(USER_ID_KEY, context.getUserId());
        params.put(PART_NUMBER_KEY, partETag.getPartNumber());
        params.put(E_TAG_KEY, partETag.getETag());
        params.put(PART_SIZE_KEY, partETag.getPartSize());
        params.put(PART_CRC_KEY, partETag.getPartCRC());

        String realPath = assembleUrl(entity.getObjectKey(), params);

        context.setRealPath(realPath);
    }


    /**
     * 获取分片上传的缓存Key
     *
     * @param identifier
     * @param userId
     * @return
     */
    private String getCacheKey(String identifier, Long userId) {
        return String.format(CACHE_KEY_TEMPLATE, identifier, userId);
    }

    /**
     * 执行文件分片合并动作
     * 下沉到底层实现
     * 1. 获取缓存信息，拿到全局uploadId
     * 2. 从上下文信息中获取所有的分片的URL，解析出需要执行文件合并请求的参数
     * 3. 执行文件合并的请求
     * 4. 清除缓存
     * 5. 设置返回结果
     *
     * @param context
     */
    @Override
    protected void doMergeFile(MergeFileContext context) throws IOException {
        String cacheKey = getCacheKey(context.getIdentifier(), context.getUserId());

        ChunkUploadEntity entity = getCache().get(cacheKey, ChunkUploadEntity.class);

        if (Objects.isNull(entity)) {
            throw new RPanFrameworkException("文件分片合并失败，文件的唯一标识为：" + context.getIdentifier());
        }

        List<String> chunkPaths = context.getRealPathList();
        List<PartETag> partETags = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(chunkPaths)) {
            partETags = chunkPaths.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(this::analysisUrlParams)
                    .filter(Objects::nonNull)
                    .filter(jsonObject -> !jsonObject.isEmpty())
                    .map(jsonObject -> new PartETag(jsonObject.getIntValue(PART_NUMBER_KEY),
                            jsonObject.getString(E_TAG_KEY),
                            jsonObject.getLongValue(PART_SIZE_KEY),
                            jsonObject.getLong(PART_CRC_KEY)))
                    .collect(Collectors.toList());
        }

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(config.getBucketName(),
                entity.getObjectKey(), entity.uploadId, partETags);
        CompleteMultipartUploadResult result = client.completeMultipartUpload(request);
        if (Objects.isNull(result)) {
            throw new RPanFrameworkException("文件分片合并失败，文件的唯一标识为：" + context.getIdentifier());
        }

        getCache().evict(cacheKey);

        context.setRealPath(entity.getObjectKey());
    }

    /**
     * 读取文件内容并写入到输出流中
     * 下沉到子类中实现
     */
    @Override
    protected void doReadFile(ReadFileContext context) throws IOException {
        OSSObject ossObject = client.getObject(config.getBucketName(), context.getRealPath());
        if (Objects.isNull(ossObject)) {
            throw new RPanFrameworkException("文件读取失败，文件的名称为：" + context.getRealPath());
        }
        FileUtils.writeStream2StreamNormal(ossObject.getObjectContent(), context.getOutputStream());
    }

    /*********************private*********************/


    /**
     * 分析URL参数
     *
     * @param url
     * @return
     */
    private JSONObject analysisUrlParams(String url) {
        JSONObject result = new JSONObject();
        if (!checkHaveParams(url)) {
            return result;
        }
        String paramsPart = url.split(getSplitMark(RPanConstants.QUESTION_MARK_STR))[1];
        if (StringUtils.isNotBlank(paramsPart)) {
            List<String> paramPairList = Splitter.on(RPanConstants.AND_MARK_STR).splitToList(paramsPart);
            paramPairList.stream().forEach(paramPair -> {
                String[] paramArr = paramPair.split(getSplitMark(RPanConstants.EQUALS_MARK_STR));
                if (paramArr != null && paramArr.length == RPanConstants.TWO_INT) {
                    result.put(paramArr[0], paramArr[1]);
                }
            });
        }
        return result;
    }

    /**
     * 初始化文件分片上传
     * 1. 执行初始化请求
     * 2. 保存初始化结果到缓存中
     *
     * @param filename
     * @param cacheKey
     * @return
     */
    private ChunkUploadEntity initChunkUpload(String filename, String cacheKey) {
        String filePath = getFilePath(filename);

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(config.getBucketName(), filePath);
        InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);

        if (Objects.isNull(result)) {
            throw new RPanFrameworkException("文件分片上传初始化失败");
        }

        ChunkUploadEntity entity = new ChunkUploadEntity();
        entity.setObjectKey(filePath);
        entity.setUploadId(result.getUploadId());

        getCache().put(cacheKey, entity);

        return entity;
    }

    /**
     * 拼接URL
     *
     * @param baseUrl
     * @param params
     * @return baseUrl?paramKey1=paramValue1&paramKey2=paramValue2
     */
    private String assembleUrl(String baseUrl, JSONObject params) {
        if (Objects.isNull(params) || params.isEmpty()) {
            return baseUrl;
        }

        StringBuffer urlStringBuffer = new StringBuffer(baseUrl);
        urlStringBuffer.append(RPanConstants.QUESTION_MARK_STR);
        List<String> paramsList = Lists.newArrayList();
        StringBuffer urlParamsStringBuffer = new StringBuffer();

        params.entrySet().forEach(entry -> {
            urlParamsStringBuffer.setLength(RPanConstants.ZERO_INT);
            urlParamsStringBuffer.append(entry.getKey());
            urlParamsStringBuffer.append(RPanConstants.EQUALS_MARK_STR);
            urlParamsStringBuffer.append(entry.getValue());
            paramsList.add(urlParamsStringBuffer.toString());
        });

        return urlStringBuffer.append(Joiner.on(RPanConstants.AND_MARK_STR)
                .join(paramsList)).toString();
    }

    /**
     * 该实体为文件分片上传树池化之后的全局信息载体
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public static class ChunkUploadEntity implements Serializable {

        private static final long serialVersionUID = -1646644290507942507L;

        /**
         * 分片上传全局唯一的uploadId
         */
        private String uploadId;

        /**
         * 文件分片上传的实体名称
         */
        private String objectKey;
    }

    /**
     * 获取基础URL
     *
     * @param url
     * @return
     */
    private String getBaseUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return RPanConstants.EMPTY_STR;
        }
        if (checkHaveParams(url)) {
            return url.split(getSplitMark(RPanConstants.QUESTION_MARK_STR))[0];
        }
        return url;
    }

    /**
     * 检查是否是含有参数的URL
     *
     * @param url
     * @return
     */
    private boolean checkHaveParams(String url) {
        return StringUtils.isNotBlank(url)
                && url.indexOf(RPanConstants.QUESTION_MARK_STR) != RPanConstants.MINUS_ONE_INT;
    }

    /**
     * 获取截取字符串的关键标识
     * 由于java的字符串分割会按照正则去截取
     * 我们的URL会影响标识的识别，故添加左右中括号去分组
     *
     * @param mark
     * @return
     */
    private String getSplitMark(String mark) {
        return new StringBuffer(RPanConstants.LEFT_BRACKET_STR)
                .append(mark)
                .append(RPanConstants.RIGHT_BRACKET_STR)
                .toString();
    }
}
