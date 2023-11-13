package com.rocket.pan.server.modules.file.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocket.pan.constants.RPanConstants;
import com.rocket.pan.exception.RPanBusinessException;
import com.rocket.pan.server.modules.file.constants.FileConstants;
import com.rocket.pan.server.modules.file.context.CreateFolderContext;
import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.rocket.pan.server.modules.file.enums.DelFlagEnum;
import com.rocket.pan.server.modules.file.enums.FolderFlagEnum;
import com.rocket.pan.server.modules.file.service.IUserFileService;
import com.rocket.pan.server.modules.file.mapper.RPanUserFileMapper;
import com.rocket.pan.util.IdUtil;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 19750
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:40:41
 */
@Service("userFileService")
public class UserFileServiceImpl extends ServiceImpl<RPanUserFileMapper, RPanUserFile>
        implements IUserFileService {

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
     * ======================private============================
     **/
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




