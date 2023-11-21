package com.rocket.pan.server.modules.file.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.rocket.pan.core.constants.RPanConstants;
import com.rocket.pan.core.exception.RPanBusinessException;
import com.rocket.pan.core.utils.FileUtils;
import com.rocket.pan.server.common.event.file.DeleteFileEvent;
import com.rocket.pan.server.common.event.search.UserSearchEvent;
import com.rocket.pan.server.common.utils.HttpUtil;
import com.rocket.pan.server.modules.file.constants.FileConstants;
import com.rocket.pan.server.modules.file.context.*;
import com.rocket.pan.server.modules.file.converter.FileConverter;
import com.rocket.pan.server.modules.file.entity.RPanFile;
import com.rocket.pan.server.modules.file.entity.RPanFileChunk;
import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.rocket.pan.server.modules.file.enums.DelFlagEnum;
import com.rocket.pan.server.modules.file.enums.FileTypeEnum;
import com.rocket.pan.server.modules.file.enums.FolderFlagEnum;
import com.rocket.pan.server.modules.file.service.IFileChunkService;
import com.rocket.pan.server.modules.file.service.IFileService;
import com.rocket.pan.server.modules.file.service.IUserFileService;
import com.rocket.pan.server.modules.file.mapper.RPanUserFileMapper;
import com.rocket.pan.server.modules.file.vo.*;
import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.storage.engine.core.StorageEngine;
import com.rocket.pan.storage.engine.core.context.ReadFileContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 19750
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:40:41
 */
@Service
public class UserFileServiceImpl extends ServiceImpl<RPanUserFileMapper, RPanUserFile>
        implements IUserFileService, ApplicationContextAware {


    private ApplicationContext applicationContext;

    @Autowired
    private IFileService iFileService;

    @Autowired
    private FileConverter fileConverter;

    @Autowired
    private IFileChunkService iFileChunkService;

    @Autowired
    private StorageEngine storageEngine;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 创建文件夹信息
     *
     * @param createFolderContext
     * @return
     */
    @Override
    public Long createFolder(CreateFolderContext createFolderContext) {

        return saveUserFile(createFolderContext.getParentId(),
                createFolderContext.getFolderName(),
                FolderFlagEnum.YES,
                null,
                null,
                createFolderContext.getUserId(),
                null);
    }

    /**
     * 查询用户的根文件夹信息
     *
     * @param userId
     * @return
     */
    @Override
    public RPanUserFile getUserRootFile(Long userId) {
        LambdaQueryWrapper<RPanUserFile> lambdaQueryWrapper = Wrappers.<RPanUserFile>lambdaQuery()
                .eq(ObjectUtils.isNotNull(userId), RPanUserFile::getUserId, userId)
                .eq(ObjectUtils.isNotNull(FileConstants.TOP_PARENT_ID), RPanUserFile::getParentId, FileConstants.TOP_PARENT_ID)
                .eq(ObjectUtils.isNotNull(DelFlagEnum.NO.getCode()), RPanUserFile::getDelFlag, DelFlagEnum.NO.getCode())
                .eq(ObjectUtils.isNotNull(FolderFlagEnum.YES.getCode()), RPanUserFile::getFolderFlag, FolderFlagEnum.YES.getCode());
        return getOne(lambdaQueryWrapper);
    }

    /**
     * 查询用户的文件列表
     *
     * @param context
     * @return
     */
    @Override
    public List<RPanUserFileVO> getFileList(QueryFileListContext context) {
        List<RPanUserFileVO> rPanUserFileVOS = baseMapper.selectFileList(context);
        System.out.println();
        return rPanUserFileVOS;
    }

    /**
     * 更新文件名称
     * 1. 校验更新文件名称的条件
     * 2. 执行更新文件名称的操作
     *
     * @param context
     */
    @Override
    public void updateFilename(UpdateFilenameContext context) {
        checkUpdateFilenameCondition(context);
        doUpdateFilename(context);
    }

    /**
     * 批量删除用户文件
     * 1. 校验删除的条件
     * 2. 执行批量删除文件操作
     * 3. 发布批量删除文件的事件，给其他模块订阅使用
     *
     * @param context
     */
    @Override
    public void deleteFile(DeleteFileContext context) {
        checkDeleteFileCondition(context);
        doDeleteFile(context);
        afterFileDelete(context);
    }

    /**
     * 文件妙传
     * 1. 通过文件唯一标识，查找对应文件实体记录
     * 2. 如果没有找到，直接返回妙传失败
     * 3. 如果查到记录，直接挂载关联关系，返回妙传成功
     *
     * @param context
     * @return
     */
    @Override
    public boolean secUpload(SecUploadFileContext context) {
        RPanFile record = getFileByUserIdAndIdentifier(context.getUserId(), context.getIdentifier());
        if (ObjectUtil.isNull(record)) {
            return false;
        }

        saveUserFile(context.getParentId(),
                context.getFilename(),
                FolderFlagEnum.NO,
                FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                record.getFileId(),
                context.getUserId(),
                record.getFileSizeDesc());

        return true;
    }

    /**
     * 单文件上传
     * 1. 上传文件并保存实体文件记录
     * 2. 保存用户文件的关系记录
     *
     * @param context
     */
    @Override
    @Transactional
    public void upload(FileUploadContext context) {
        saveFile(context);
        saveUserFile(context.getParentId(), context.getFilename(), FolderFlagEnum.NO,
                FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                context.getRecord().getFileId(),
                context.getUserId(),
                context.getRecord().getFileSizeDesc());
    }

    /**
     * 文件分片上传
     * 1. 上传实体文件
     * 2. 保存分片文件记录
     * 3. 校验是否全部分片上传完成
     *
     * @param context
     * @return
     */
    @Override
    public FileChunkUploadVO chunkUpload(FileChunkUploadContext context) {
        FileChunkSaveContext fileChunkSaveContext = fileConverter.fileChunkUploadContext2FileChunkSaveContext(context);
        iFileChunkService.saveChunkFile(fileChunkSaveContext);
        FileChunkUploadVO fileChunkUploadVO = new FileChunkUploadVO();
        fileChunkUploadVO.setMergeFlag(fileChunkSaveContext.getMergeFlagEnum().getCode());
        return fileChunkUploadVO;
    }


    /**
     * 通过用户id和标识符获取文件对象
     *
     * @param userId
     * @param identifier
     * @return
     */
    private RPanFile getFileByUserIdAndIdentifier(Long userId, String identifier) {
        LambdaQueryWrapper<RPanFile> lambdaQueryWrapper = Wrappers.<RPanFile>lambdaQuery()
                .eq(RPanFile::getCreateUser, userId)
                .eq(RPanFile::getIdentifier, identifier);
        List<RPanFile> records = iFileService.list(lambdaQueryWrapper);

        if (CollectionUtil.isEmpty(records)) {
            return null;
        }

        return records.get(RPanConstants.ZERO_INT);
    }

    /**
     * 文件批量删除的后置操作
     * 1. 对外发布文件删除的事件
     *
     * @param context
     */
    private void afterFileDelete(DeleteFileContext context) {
        DeleteFileEvent deleteFileEvent = new DeleteFileEvent(this, context.getFileIdList());
        applicationContext.publishEvent(deleteFileEvent);
    }

    /**
     * 执行文件删除的操作
     * 1. 删除为逻辑删除
     *
     * @param context
     */
    private void doDeleteFile(DeleteFileContext context) {
        LambdaUpdateWrapper<RPanUserFile> lambdaUpdateWrapper = Wrappers.<RPanUserFile>lambdaUpdate()
                .in(RPanUserFile::getFileId, context.getFileIdList())
                .set(RPanUserFile::getDelFlag, DelFlagEnum.YES.getCode())
                .set(RPanUserFile::getUpdateTime, new Date());

        if (!update(lambdaUpdateWrapper)) {
            throw new RPanBusinessException("文件删除失败");
        }
    }


    /**
     * 删除文件的前置校验
     * 1. 文件ID合法校验
     * 2. 用户拥有删除文件的权限
     *
     * @param context
     */
    private void checkDeleteFileCondition(DeleteFileContext context) {
        List<Long> fileIdList = context.getFileIdList();

        List<RPanUserFile> rPanUserFileList = listByIds(fileIdList);
        if (fileIdList.size() != rPanUserFileList.size()) {
            throw new RPanBusinessException("存在不合法的文件记录");
        }

        Set<Long> fileIdSet = rPanUserFileList.stream().map(RPanUserFile::getFileId).collect(Collectors.toSet());
        int oldSize = fileIdSet.size();
        fileIdSet.addAll(fileIdList);
        int newSize = fileIdSet.size();

        if (oldSize != newSize) {
            throw new RPanBusinessException("存在不合法的文件记录");
        }

        Set<Long> userIdSet = rPanUserFileList.stream().map(RPanUserFile::getUserId).collect(Collectors.toSet());
        if (userIdSet.size() != 1) {
            throw new RPanBusinessException("存在不合法的文件记录");
        }

        Long dbUserId = userIdSet.stream().findFirst().get();
        if (ObjectUtil.notEqual(dbUserId, context.getUserId())) {
            throw new RPanBusinessException("当前登录用户没有删除文件的权限");
        }
    }

    /**
     * 执行文件重命名操作
     *
     * @param context
     */
    private void doUpdateFilename(UpdateFilenameContext context) {
        RPanUserFile entity = context.getEntity();
        entity.setFilename(context.getNewFilename());
        entity.setUpdateUser(context.getUserId());
        entity.setUpdateTime(new Date());

        if (!updateById(entity)) {
            throw new RPanBusinessException("文件重命名失败");
        }
    }

    /**
     * 更新文件名称的条件校验
     * 1. 文件ID是有效的
     * 2. 用户有权限更新改文件的文件名称
     * 3. 新旧文件名称不能相同
     * 4. 不能使用当前文件夹下的子文件的名称
     *
     * @param context
     */
    private void checkUpdateFilenameCondition(UpdateFilenameContext context) {
        Long fileId = context.getFileId();
        RPanUserFile entity = getById(fileId);

        if (ObjectUtil.isNull(entity)) {
            throw new RPanBusinessException("该文件ID无效");
        }

        if (ObjectUtil.notEqual(context.getUserId(), entity.getUserId())) {
            throw new RPanBusinessException("该用户没有权限修改该文件的名称");
        }

        if (ObjectUtil.equal(context.getNewFilename(), entity.getFilename())) {
            throw new RPanBusinessException("新旧文件名称不能相同，请换一个新的文件名称");
        }

        LambdaQueryWrapper<RPanUserFile> lambdaQueryWrapper = Wrappers.<RPanUserFile>lambdaQuery()
                .eq(RPanUserFile::getParentId, entity.getParentId())
                .eq(RPanUserFile::getFilename, context.getNewFilename());
        int count = count(lambdaQueryWrapper);
        if (count > 0) {
            throw new RPanBusinessException("该文件名称已经被占用，请换一个新的文件名称");
        }

        context.setEntity(entity);
    }

    /**
     * 查询用户已上传的分片列表
     * 1. 查询已上传的文件列表
     * 2. 封装返回实体
     *
     * @param context
     * @return
     */
    @Override
    public UploadedChunksVO getUploadedChunks(QueryUploadedChunksContext context) {
        LambdaQueryWrapper<RPanFileChunk> lambdaQueryWrapper = Wrappers.<RPanFileChunk>lambdaQuery()
                .select(RPanFileChunk::getChunkNumber)
                .eq(RPanFileChunk::getIdentifier, context.getIdentifier())
                .eq(RPanFileChunk::getCreateUser, context.getUserId())
                .gt(RPanFileChunk::getExpirationTime, new Date());
        List<Integer> uploadedChunks = iFileChunkService.listObjs(lambdaQueryWrapper, value -> (Integer) value);

        UploadedChunksVO uploadedChunksVO = new UploadedChunksVO();
        uploadedChunksVO.setUploadedChunks(uploadedChunks);

        return uploadedChunksVO;
    }

    /**
     * 文件分片合并
     * 1. 文件分片物理合并
     * 2. 保存文件实体记录
     * 3. 保存文件用户关系映射
     *
     * @param context
     */
    @Override
    public void mergeFile(FileChunkMergeContext context) {
        mergeFileChunkAndSaveFile(context);
        saveUserFile(context.getParentId(),
                context.getFilename(),
                FolderFlagEnum.NO,
                FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                context.getRecord().getFileId(),
                context.getUserId(),
                context.getRecord().getFileSizeDesc());
    }

    /**
     * 文件下载
     * 1. 参数校验：校验文件是否存在，校验文件是否属于该用户
     * 2. 校验该文件是不是文件夹
     * 3. 执行下载的动作
     *
     * @param context
     */
    @Override
    public void download(FileDownloadContext context) {
        RPanUserFile record = getById(context.getFileId());
        checkOperatePermission(record, context.getUserId());
        if (checkIsFolder(record)) {
            throw new RPanBusinessException("文件夹暂时不支持下载");
        }

        doDownload(record, context.getResponse());
    }

    /**
     * 文件预览
     * 1. 参数校验：校验文件是否存在，文件是否属于该用户
     * 2. 校验该文件是不是文件夹
     * 3. 执行文件预览的动作
     *
     * @param context
     */
    @Override
    public void preview(FilePreviewContext context) {
        RPanUserFile record = getById(context.getFileId());
        checkOperatePermission(record, context.getUserId());
        if (checkIsFolder(record)) {
            throw new RPanBusinessException("文件夹暂时不支持下载");
        }

        doPreview(record, context.getResponse());
    }

    /**
     * 查询用户的文件夹树
     * 不推荐使用递归查询，每次递归都需要查询数据库，在接口总耗时里面是非常耗时的，能一次链接搞定就一次搞定
     * 实现业务简单、效率高、占用资源少，评判接口好坏的指标
     * <p>
     * 1. 查询出该用户的所有文件夹列表
     * 2. 在内存中拼接文件夹树
     *
     * @param context
     * @return
     */
    @Override
    public List<FolderTreeNodeVO> getFolderTree(QueryFolderTreeContext context) {
        List<RPanUserFile> folderRecords = queryFolderRecords(context.getUserId());
        List<FolderTreeNodeVO> result = assembleFolderTreeNodeVOList(folderRecords);
        return result;
    }

    /**
     * 文件转移
     * 1. 权限校验
     * 2. 执行工作
     *
     * @param context
     */
    @Override
    public void transfer(TransferFileContext context) {
        checkTransferCondition(context);
        doTransfer(context);
    }

    /**
     * 文件复制
     * 1. 条件校验
     * 2. 执行动作
     *
     * @param context
     */
    @Override
    public void copy(CopyFileContext context) {
        checkCopyCondition(context);
        doCopy(context);
    }

    /**
     * 文件列表搜索
     * 1. 执行文件搜索
     * 2. 拼装文件的父文件夹名称
     * 3. 执行文件搜索后的后置操作
     *
     * @param context
     * @return
     */
    @Override
    public List<FileSearchResultVO> search(FileSearchContext context) {
        List<FileSearchResultVO> result = doSearch(context);
        fillParentFilename(result);
        afterSearch(context);
        return result;
    }

    /**
     * 获取面包屑列表
     * 1. 获取用户所有的文件夹信息
     * 2. 拼接需要使用到的面包屑列表
     *
     * @param context
     * @return
     */
    @Override
    public List<BreadcrumbVO> getBreadcrumbs(QueryBreadcrumbsContext context) {
        List<RPanUserFile> folderRecords = queryFolderRecords(context.getUserId());
        Map<Long, BreadcrumbVO> prepareBreadcrumbVOMap = folderRecords.stream()
                .map(BreadcrumbVO::transfer)
                .collect(Collectors.toMap(BreadcrumbVO::getId, a -> a));

        BreadcrumbVO currentNode;
        Long fileId = context.getFileId();
        LinkedList<BreadcrumbVO> result = Lists.newLinkedList();
        do {
            currentNode = prepareBreadcrumbVOMap.get(fileId);
            if (ObjectUtil.isNotNull(currentNode)) {
                result.add(0, currentNode);
                fileId = currentNode.getParentId();
            }
        } while (ObjectUtil.isNotNull(currentNode));

        return result;
    }

    /**
     * 搜索的后置操作
     * 1. 发布文件的搜索的事件
     * 2.
     *
     * @param context
     */
    private void afterSearch(FileSearchContext context) {
        UserSearchEvent event = new UserSearchEvent(this, context.getKeyword(), context.getUserId());
        applicationContext.publishEvent(event);
    }

    /**
     * 填充文件列表的父文件名称
     *
     * @param result
     */
    private void fillParentFilename(List<FileSearchResultVO> result) {
        if (CollectionUtil.isEmpty(result)) {
            return;
        }

        List<Long> parentIdList = result.stream().map(FileSearchResultVO::getParentId).collect(Collectors.toList());
        List<RPanUserFile> parentRecords = listByIds(parentIdList);
        Map<Long, String> fileId2FilenameMap = parentRecords.stream().collect(Collectors.toMap(RPanUserFile::getFileId, RPanUserFile::getFilename));

        result.stream().forEach(vo -> vo.setParentFilename(fileId2FilenameMap.get(vo.getParentId())));
    }

    /**
     * 搜索文件列表
     *
     * @param context
     * @return
     */
    private List<FileSearchResultVO> doSearch(FileSearchContext context) {
        return baseMapper.searchFile(context);
    }

    /**
     * 执行文件复制的动作
     *
     * @param context
     */
    private void doCopy(CopyFileContext context) {
        List<RPanUserFile> prepareRecords = context.getPrepareRecords();
        if (CollectionUtils.isNotEmpty(prepareRecords)) {
            List<RPanUserFile> allRecords = Lists.newArrayList();
            prepareRecords.stream().forEach(record -> assembleCopyChildRecord(allRecords, record, context.getTargetParentId(), context.getUserId()));
            if (!saveBatch(allRecords)) {
                throw new RPanBusinessException("文件复制失败");
            }
        }
    }

    /**
     * 拼装当前文件记录以及所有的子文件记录
     *
     * @param allRecords
     * @param record
     * @param targetParentId
     * @param userId
     */
    private void assembleCopyChildRecord(List<RPanUserFile> allRecords, RPanUserFile record, Long targetParentId, Long userId) {
        Long newFileId = IdUtil.get();
        Long oldFileId = record.getFileId();

        record.setParentId(targetParentId);
        record.setFileId(newFileId);
        record.setUserId(userId);
        record.setCreateUser(userId);
        record.setCreateTime(new Date());
        record.setUpdateUser(userId);
        record.setUpdateTime(new Date());
        handleDuplicateFilename(record);

        allRecords.add(record);

        if (checkIsFolder(record)) {
            List<RPanUserFile> childRecords = findChildRecords(oldFileId);
            if (CollectionUtils.isEmpty(childRecords)) {
                return;
            }
            childRecords.stream().forEach(childRecord -> assembleCopyChildRecord(allRecords, childRecord, newFileId, userId));
        }
    }

    /**
     * 查找下一级的文件记录
     *
     * @param parentId
     * @return
     */
    private List<RPanUserFile> findChildRecords(Long parentId) {
        LambdaQueryWrapper<RPanUserFile> lambdaQueryWrapper = Wrappers.<RPanUserFile>lambdaQuery()
                .eq(RPanUserFile::getParentId, parentId)
                .eq(RPanUserFile::getDelFlag, DelFlagEnum.NO.getCode());
        return list(lambdaQueryWrapper);
    }

    /**
     * 文件转移的条件校验
     * 1. 目标文件必须是一个文件夹
     * 2. 选中的要转移的文件列表中不能含有目标文件夹及其子文件夹
     *
     * @param context
     */
    private void checkCopyCondition(CopyFileContext context) {
        Long targetParentId = context.getTargetParentId();
        if (!checkIsFolder(getById(targetParentId))) {
            throw new RPanBusinessException("目标文件不是一个文件夹");
        }
        List<Long> fileIdList = context.getFileIdList();
        List<RPanUserFile> prepareRecords = listByIds(fileIdList);
        context.setPrepareRecords(prepareRecords);
        if (checkIsChildFolder(prepareRecords, targetParentId, context.getUserId())) {
            throw new RPanBusinessException("目标文件夹ID不能是选中文件列表的文件夹ID或其子文件夹ID");
        }
    }

    /**
     * 执行文件转移操作
     *
     * @param context
     */
    private void doTransfer(TransferFileContext context) {
        List<RPanUserFile> prepareRecords = context.getPrepareRecords();
        prepareRecords.stream().forEach(record -> {
            record.setParentId(context.getTargetParentId());
            record.setUserId(context.getUserId());
            record.setCreateUser(context.getUserId());
            record.setCreateTime(new Date());
            record.setUpdateUser(context.getUserId());
            record.setUpdateTime(new Date());
            handleDuplicateFilename(record);
        });
        if (!updateBatchById(prepareRecords)) {
            throw new RPanBusinessException("文件转移失败");
        }
    }

    /**
     * 文件转移的条件校验
     *
     * @param context
     */
    private void checkTransferCondition(TransferFileContext context) {
        Long targetParentId = context.getTargetParentId();

        if (!checkIsFolder(getById(targetParentId))) {
            throw new RPanBusinessException("目标文件不是一个文件夹");
        }

        List<Long> fileIdList = context.getFileIdList();
        List<RPanUserFile> prepareRecords = listByIds(fileIdList);
        context.setPrepareRecords(prepareRecords);
        if (checkIsChildFolder(prepareRecords, targetParentId, context.getUserId())) {
            throw new RPanBusinessException("目标文件夹ID不能是选中文件列表的文件夹ID及其子文件夹ID");
        }
    }

    /**
     * 校验目标文件夹ID都是要操作的文件记录的文件夹ID以及其子文件夹ID
     *
     * @param prepareRecords
     * @param targetParentId
     * @param userId
     * @return
     */
    private boolean checkIsChildFolder(List<RPanUserFile> prepareRecords, Long targetParentId, Long userId) {
        prepareRecords = prepareRecords.stream()
                .filter(record -> Objects.equals(record.getFolderFlag(), FolderFlagEnum.YES.getCode()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(prepareRecords)) {
            return false;
        }

        List<RPanUserFile> folderRecords = queryFolderRecords(userId);
        Map<Long, List<RPanUserFile>> folderRecordMap = folderRecords.stream()
                .collect(Collectors.groupingBy(RPanUserFile::getParentId));

        List<RPanUserFile> unavailableFolderRecords = Lists.newArrayList();

        unavailableFolderRecords.addAll(prepareRecords);
        prepareRecords.stream().forEach(record -> findAllChildFolderRecords(unavailableFolderRecords, folderRecordMap, record));

        List<Long> unavailableFolderRecordIds = unavailableFolderRecords.stream()
                .map(RPanUserFile::getFileId).collect(Collectors.toList());

        return unavailableFolderRecordIds.contains(targetParentId);
    }

    /**
     * 查找文件夹的所有子文件夹记录
     *
     * @param unavailableFolderRecords
     * @param folderRecordMap
     * @param record
     */
    private void findAllChildFolderRecords(List<RPanUserFile> unavailableFolderRecords, Map<Long, List<RPanUserFile>> folderRecordMap, RPanUserFile record) {
        if (Objects.isNull(record)) {
            return;
        }
        List<RPanUserFile> childFolderRecords = folderRecordMap.get(record.getFileId());
        if (CollectionUtils.isEmpty(childFolderRecords)) {
            return;
        }
        unavailableFolderRecords.addAll(childFolderRecords);
        childFolderRecords.stream().forEach(childRecord -> findAllChildFolderRecords(unavailableFolderRecords, folderRecordMap, childRecord));
    }

    private List<FolderTreeNodeVO> assembleFolderTreeNodeVOList(List<RPanUserFile> folderRecords) {
        if (CollectionUtil.isEmpty(folderRecords)) {
            return Lists.newArrayList();
        }

        List<FolderTreeNodeVO> mappedFolderTreeNodeVOList = folderRecords.stream()
                .map(fileConverter::rPanUserFile2FolderTreeNodeVO).collect(Collectors.toList());
        // 这步分组很关键
        // 只是做了引用的复制没有在堆里创建新的对象，所有的过程都是对象的copy操作，没有改变对象的位置也没有生成新的对象
        Map<Long, List<FolderTreeNodeVO>> mappedFolderTreeNodeVOMap = mappedFolderTreeNodeVOList.stream().collect(Collectors.groupingBy(FolderTreeNodeVO::getParentId));

        for (FolderTreeNodeVO node : mappedFolderTreeNodeVOList) {
            List<FolderTreeNodeVO> children = mappedFolderTreeNodeVOMap.get(node.getId());
            if (CollectionUtil.isNotEmpty(children)) {
                node.getChildren().addAll(children);
            }
        }

        return mappedFolderTreeNodeVOList.stream()
                .filter(node -> ObjectUtil.equal(node.getParentId(), FileConstants.TOP_PARENT_ID))
                .collect(Collectors.toList());
    }

    /**
     * 查询出该用户的所有文件夹列表
     *
     * @param userId
     * @return
     */
    private List<RPanUserFile> queryFolderRecords(Long userId) {
        LambdaQueryWrapper<RPanUserFile> lambdaQueryWrapper = Wrappers.<RPanUserFile>lambdaQuery()
                .eq(RPanUserFile::getUserId, userId)
                .eq(RPanUserFile::getFolderFlag, FolderFlagEnum.YES.getCode())
                .eq(RPanUserFile::getDelFlag, DelFlagEnum.NO.getCode());
        return list(lambdaQueryWrapper);
    }


    /**
     * 执行文件预览的动作
     * 1. 查询文件的真实存储路径
     * 2. 添加跨域的公共响应头
     * 3. 委托文件存储引擎去读取文件内容到响应的输出流
     *
     * @param record
     * @param response
     */
    private void doPreview(RPanUserFile record, HttpServletResponse response) {
        RPanFile readFileRecord = iFileService.getById(record.getRealFileId());

        if (ObjectUtil.isNull(readFileRecord)) {
            throw new RPanBusinessException("当前的文件记录不存在");
        }

        addCommonResponseHeader(response, readFileRecord.getFilePreviewContentType());
        readFile2OutputStream(readFileRecord.getRealPath(), response);
    }

    /**
     * 文件下载
     * 1. 查询文件的存储路径
     * 2. 添加跨域的公共响应头
     * 3. 拼装下载的文件的名称、长度等响应信息
     * 4. 委托文件引擎去读取文件内容到响应的输出流中
     *
     * @param record
     * @param response
     */
    private void doDownload(RPanUserFile record, HttpServletResponse response) {
        RPanFile readFileRecord = iFileService.getById(record.getRealFileId());

        if (ObjectUtil.isNull(readFileRecord)) {
            throw new RPanBusinessException("当前的文件记录不存在");
        }
        addCommonResponseHeader(response, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        addDownloadAttribute(response, record, readFileRecord);
        readFile2OutputStream(readFileRecord.getRealPath(), response);
    }

    /**
     * 委托文件存储引擎读取文件内容并写入到输出流中
     *
     * @param realPath
     * @param response
     */
    private void readFile2OutputStream(String realPath, HttpServletResponse response) {
        try {
            ReadFileContext context = new ReadFileContext();
            context.setRealPath(realPath);
            context.setOutputStream(response.getOutputStream());
            storageEngine.readFile(context);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件下载失败");
        }
    }

    /**
     * 添加文件下载的属性信息
     *
     * @param response
     * @param record
     * @param realFileRecord
     */
    private void addDownloadAttribute(HttpServletResponse response, RPanUserFile record, RPanFile realFileRecord) {
        try {
            response.addHeader(FileConstants.CONTENT_DISPOSITION_VALUE_PREFIX_STR,
                    FileConstants.CONTENT_DISPOSITION_VALUE_PREFIX_STR + new String(record.getFilename().getBytes(FileConstants.GB2312_STR),
                            FileConstants.IOS_8859_1_STR));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件下载失败");
        }
        response.setContentLengthLong(Long.valueOf(realFileRecord.getFileSize()));
    }

    /**
     * 添加公共的文件读取响应头
     *
     * @param response
     * @param contentTypeValue
     */
    private void addCommonResponseHeader(HttpServletResponse response, String contentTypeValue) {
        // 重置Http响应对象的状态
        response.reset();
        HttpUtil.addCorsResponseHeaders(response);
        response.addHeader(FileConstants.CONTENT_TYPE_STR, contentTypeValue);
        response.setContentType(contentTypeValue);
    }

    /**
     * 校验该文件是不是文件夹
     *
     * @param record
     * @return
     */
    private boolean checkIsFolder(RPanUserFile record) {
        if (ObjectUtil.isNull(record)) {
            throw new RPanBusinessException("当前文件记录不存在");
        }
        return FolderFlagEnum.YES.getCode().equals(record.getFolderFlag());
    }

    /**
     * 校验用户操作权限
     * 1. 文件记录必须存在
     * 2. 文件记录的创建者必须是该用户
     *
     * @param record
     * @param userId
     */
    private void checkOperatePermission(RPanUserFile record, Long userId) {
        if (ObjectUtil.isNull(record)) {
            throw new RPanBusinessException("当前文件记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new RPanBusinessException("你没有操作该文件的权限");
        }
    }


    /**
     * ======================private============================
     **/
    /**
     * 合并文件分片并保存物理文件记录
     *
     * @param context
     */
    private void mergeFileChunkAndSaveFile(FileChunkMergeContext context) {
        FileChunkMergeAndSaveContext fileChunkMergeAndSaveContext = fileConverter.fileChunkMergeContext2FileChunkMergeAndSaveContext(context);
        iFileService.mergeFileChunkAndSaveFile(fileChunkMergeAndSaveContext);
        context.setRecord(fileChunkMergeAndSaveContext.getRecord());
    }


    /**
     * 上传文件并保存实体文件记录
     * 委托给实体文件的Service去完成该操作
     *
     * @param context
     */
    private void saveFile(FileUploadContext context) {
        FileSaveContext fileSaveContext = fileConverter.fileUploadContext2FileSaveContext(context);
        iFileService.saveFile(fileSaveContext);
        context.setRecord(fileSaveContext.getRecord());
    }

    /**
     * 保存用户文件的映射记录
     *
     * @param parentId
     * @param filename
     * @param folderFlagEnum
     * @param fileType
     * @param realFileId
     * @param userId
     * @param fileSizeDesc
     * @return
     */
    private Long saveUserFile(Long parentId,
                              String filename,
                              FolderFlagEnum folderFlagEnum,
                              Integer fileType,
                              Long realFileId,
                              Long userId,
                              String fileSizeDesc) {
        RPanUserFile entity = assembleRPanFUserFile(parentId, userId, filename, folderFlagEnum, fileType, realFileId, fileSizeDesc);
        if (!save((entity))) {
            throw new RPanBusinessException("保存文件信息失败");
        }
        return entity.getFileId();
    }

    /**
     * 用户文件映射关系实体转化
     * 1. 构建并填充实体
     * 2. 处理文件命名一致的问题  b b（1） b（2） 参考windows
     *
     * @param parentId
     * @param userId
     * @param filename
     * @param folderFlagEnum
     * @param fileType
     * @param realFileId
     * @param fileSizeDesc
     * @return
     */
    private RPanUserFile assembleRPanFUserFile(Long parentId, Long userId, String filename, FolderFlagEnum folderFlagEnum, Integer fileType, Long realFileId, String fileSizeDesc) {
        RPanUserFile entity = new RPanUserFile();

        entity.setFileId(IdUtil.get());
        entity.setUserId(userId);
        entity.setParentId(parentId);
        entity.setRealFileId(realFileId);
        entity.setFilename(filename);
        entity.setFolderFlag(folderFlagEnum.getCode());
        entity.setFileSizeDesc(fileSizeDesc);
        entity.setFileType(fileType);
        entity.setDelFlag(DelFlagEnum.NO.getCode());
        entity.setCreateUser(userId);
        entity.setCreateTime(new Date());
        entity.setUpdateUser(userId);
        entity.setUpdateTime(new Date());

        // 处理重复名称
        handleDuplicateFilename(entity);

        return entity;
    }

    /**
     * 处理用户重复名称
     * 如果同一个文件夹下有文件名重复
     * 按照系统级规则重命名文件
     *
     * @param entity
     */
    private void handleDuplicateFilename(RPanUserFile entity) {
        String filename = entity.getFilename();
        String newFilenameWithoutSuffix;
        String newFilenameSuffix;
        int newFilenamePointPosition = filename.lastIndexOf(RPanConstants.POINT_STR);
        if (newFilenamePointPosition == RPanConstants.MINUS_ONE_INT) {
            newFilenameWithoutSuffix = filename;
            newFilenameSuffix = StringUtils.EMPTY;
        } else {
            newFilenameWithoutSuffix = filename.substring(RPanConstants.ZERO_INT, newFilenamePointPosition);
            newFilenameSuffix = filename.replace(newFilenameWithoutSuffix, StringUtils.EMPTY);
        }

        List<RPanUserFile> existRecords = getDuplicateFilename(entity, newFilenameWithoutSuffix);
        if (CollectionUtil.isEmpty(existRecords)) {
            return;
        }

        List<String> existFilenames = existRecords.stream()
                .map(RPanUserFile::getFilename)
                .collect(Collectors.toList());

        int count = 1;
        String newFilename;
        do {
            newFilename = assembleNewFilename(newFilenameWithoutSuffix, count, newFilenameSuffix);
            count++;
        } while (existFilenames.contains(newFilename));

        entity.setFilename(newFilename);
    }

    /**
     * 拼装新文件名称
     * 拼装规则参考windows重复文件名称的重命名规范
     *
     * @param newFilenameWithoutSuffix
     * @param count
     * @param newFilenameSuffix
     * @return
     */
    private String assembleNewFilename(String newFilenameWithoutSuffix, int count, String newFilenameSuffix) {
        String newFilename = new StringBuilder(newFilenameWithoutSuffix)
                .append(FileConstants.CN_LEFT_PARENTHESES_STR)
                .append(count)
                .append(FileConstants.CN_RIGHT_PARENTHESES_STR)
                .append(newFilenameSuffix)
                .toString();
        return newFilename;
    }

    /**
     * 查找同一父文件夹下同名文件数量
     *
     * @param entity
     * @param newFilenameWithoutSuffix
     * @return
     */
    private List<RPanUserFile> getDuplicateFilename(RPanUserFile entity, String newFilenameWithoutSuffix) {
//        QueryWrapper queryWrapper = new QueryWrapper();
//        queryWrapper.eq("parent_id", entity.getParentId());
//        queryWrapper.eq("folder_flag", entity.getFolderFlag());
//        queryWrapper.eq("user_id", entity.getUserId());
//        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
//        queryWrapper.likeRight("filename", newFilenameWithoutSuffix);
//        return list(queryWrapper);

        Long parentId = entity.getParentId();
        Integer folderFlag = entity.getFolderFlag();
        Long userId = entity.getUserId();
        Integer delFlag = DelFlagEnum.NO.getCode();

        LambdaQueryWrapper<RPanUserFile> lambdaQueryWrapper = Wrappers.<RPanUserFile>lambdaQuery()
                .eq(ObjectUtils.isNotEmpty(parentId), RPanUserFile::getParentId, parentId)
                .eq(ObjectUtils.isNotEmpty(folderFlag), RPanUserFile::getFolderFlag, folderFlag)
                .eq(ObjectUtils.isNotEmpty(userId), RPanUserFile::getUserId, userId)
                .eq(ObjectUtils.isNotEmpty(delFlag), RPanUserFile::getDelFlag, delFlag)
                .likeRight(ObjectUtils.isNotEmpty(newFilenameWithoutSuffix), RPanUserFile::getFilename, newFilenameWithoutSuffix);
        return list(lambdaQueryWrapper);
    }
}




