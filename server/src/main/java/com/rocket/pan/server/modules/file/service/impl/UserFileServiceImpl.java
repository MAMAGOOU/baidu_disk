package com.rocket.pan.server.modules.file.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.core.constants.RPanConstants;
import com.rocket.pan.core.exception.RPanBusinessException;
import com.rocket.pan.core.response.R;
import com.rocket.pan.core.utils.FileUtils;
import com.rocket.pan.server.common.event.file.DeleteFileEvent;
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
import com.rocket.pan.server.modules.file.vo.FileChunkUploadVO;
import com.rocket.pan.server.modules.file.vo.RPanUserFileVO;
import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.server.modules.file.vo.UploadedChunksVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 19750
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:40:41
 */
@Service("userFileService")
public class UserFileServiceImpl extends ServiceImpl<RPanUserFileMapper, RPanUserFile>
        implements IUserFileService, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private IFileService iFileService;

    @Autowired
    private FileConverter fileConverter;

    @Autowired
    private IFileChunkService iFileChunkService;

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
        return baseMapper.selectFileList(context);
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




