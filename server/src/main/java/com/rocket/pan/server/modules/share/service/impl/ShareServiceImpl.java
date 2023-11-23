package com.rocket.pan.server.modules.share.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rocket.pan.core.constants.RPanConstants;
import com.rocket.pan.core.exception.RPanBusinessException;
import com.rocket.pan.core.response.ResponseCode;
import com.rocket.pan.core.utils.IdUtil;
import com.rocket.pan.core.utils.JwtUtil;
import com.rocket.pan.core.utils.UUIDUtil;
import com.rocket.pan.server.common.config.PanServerConfig;
import com.rocket.pan.server.common.event.log.ErrorLogEvent;
import com.rocket.pan.server.modules.enums.ShareDayTypeEnum;
import com.rocket.pan.server.modules.enums.ShareStatusEnum;
import com.rocket.pan.server.modules.file.constants.FileConstants;
import com.rocket.pan.server.modules.file.context.CopyFileContext;
import com.rocket.pan.server.modules.file.context.FileDownloadContext;
import com.rocket.pan.server.modules.file.context.QueryFileListContext;
import com.rocket.pan.server.modules.file.entity.RPanUserFile;
import com.rocket.pan.server.modules.file.enums.DelFlagEnum;
import com.rocket.pan.server.modules.file.service.IUserFileService;
import com.rocket.pan.server.modules.file.vo.RPanUserFileVO;
import com.rocket.pan.server.modules.share.constants.ShareConstants;
import com.rocket.pan.server.modules.share.context.*;
import com.rocket.pan.server.modules.share.entity.RPanShare;
import com.rocket.pan.server.modules.share.entity.RPanShareFile;
import com.rocket.pan.server.modules.share.service.IShareFileService;
import com.rocket.pan.server.modules.share.service.IShareService;
import com.rocket.pan.server.modules.share.mapper.RPanShareMapper;
import com.rocket.pan.server.modules.user.entity.RPanUser;
import com.rocket.pan.server.modules.user.service.IUserService;
import com.rocket.pan.server.modules.vo.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.mapstruct.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 19750
 * @description 针对表【r_pan_share(用户分享表)】的数据库操作Service实现
 * @createDate 2023-11-11 14:45:54
 */
@Service
public class ShareServiceImpl extends ServiceImpl<RPanShareMapper, RPanShare> implements IShareService, ApplicationContextAware {

    @Autowired
    private PanServerConfig config;

    @Autowired
    private IShareFileService iShareFileService;

    @Autowired
    private IUserFileService iUserFileService;

    @Autowired
    private IUserService iUserService;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext){
        this.applicationContext=applicationContext;
    }

//    @Autowired
//    @Qualifier(value = "defaultStreamProducer")
//    private IStreamProducer producer;

//    @Autowired
//    @Qualifier(value = "shareManualCacheService")
//    private ManualCacheService<RPanShare> cacheService;

//    @Autowired
//    private BloomFilterManager manager;

    private static final String BLOOM_FILTER_NAME = "SHARE_SIMPLE_DETAIL";

    /**
     * 创建分享链接
     * 1. 创建拼装分享实体保存到数据库
     * 2. 保存分享和对应文件的关联关系
     * 3. 拼装返回实体并返回
     *
     * @param context
     * @return
     */
    @Transactional(rollbackFor = RPanBusinessException.class)
    @Override
    public RPanShareUrlVO create(CreateShareUrlContext context) {
        saveShare(context);
        saveShareFiles(context);
        RPanShareUrlVO vo = assembleShareVO(context);
        //afterCreate(context, vo);
        return vo;
    }

    /**
     * 查询用户分享列表
     *
     * @param context
     * @return
     */
    @Override
    public List<RPanShareUrlListVO> getShares(QueryShareListContext context) {
        return baseMapper.selectShareVOListByUserId(context.getUserId());
    }

    /**
     * 取消分享链接
     * <p>
     * 1. 校验用户操作权限
     * 2. 删除对应的分享记录
     * 3. 删除对应的分享文件关联关系记录
     *
     * @param context
     */
    @Override
    public void cancelShare(CancelShareContext context) {
        checkUserCancelSharePermission(context);
        doCancelShare(context);
        doCancelShareFiles(context);
    }

    /**
     * 校验分享码
     * <p>
     * 1. 检查分享的状态是否正常
     * 2. 校验分享的分享码是否正确
     * 3. 生成一个短时间的分享token 返回给上游
     *
     * @param context
     * @return
     */
    @Override
    public String checkShareCode(CheckShareCodeContext context) {
        RPanShare record = checkShareStatus(context.getShareId());
        context.setRecord(record);
        doCheckShareCode(context);
        return generateShareToken(context);
    }

    /**
     * 查询分享详情
     * <p>
     * 1. 校验分享的状态
     * 2. 初始化分享实体
     * 3. 查询分享的主题信息
     * 4. 查询分享的文件列表
     * 5. 查询分享者的信息
     *
     * @param context
     * @return
     */
    @Override
    public ShareDetailVO detail(QueryShareDetailContext context) {
        RPanShare record = checkShareStatus(context.getShareId());
        context.setRecord(record);
        initShareVO(context);
        assembleMainShareInfo(context);
        assembleShareFilesInfo(context);
        assembleShareUserInfo(context);
        return context.getVo();
    }

    /**
     * 查询分享的简单详情
     * <p>
     * 1. 校验分享的状态
     * 2. 初始化分享实体
     * 3. 查询分享的主题信息
     * 4. 查询分享者的信息
     *
     * @param context
     * @return
     */
    @Override
    public ShareSimpleDetailVO simpleDetail(QueryShareSimpleDetailContext context) {
        RPanShare record = checkShareStatus(context.getShareId());
        context.setRecord(record);
        initShareSimpleVO(context);
        assembleMainShareSimpleInfo(context);
        assembleShareSimpleUserInfo(context);
        return context.getVo();
    }

    /**
     * 获取下一级的文件列表
     * <p>
     * 1. 校验分享状态
     * 2. 校验文件的ID是否在分享的文件列表中
     * 3. 查询对应的子文件列表，返回
     *
     * @param context
     * @return
     */
    @Override
    public List<RPanUserFileVO> fileList(QueryChildFileListContext context) {
        RPanShare record = checkShareStatus(context.getShareId());
        context.setRecord(record);
        List<RPanUserFileVO> allUserFileRecords = checkFileIdIsOnShareStatusAndGetAllShareUserFiles(context.getShareId(),
                Lists.newArrayList(context.getParentId()));

        // 对parentId进行分组，key -》parentId value -》下一级文件目录列表
        Map<Long, List<RPanUserFileVO>> parentIdFileListMap = allUserFileRecords.stream()
                .collect(Collectors.groupingBy(RPanUserFileVO::getParentId));

        List<RPanUserFileVO> rPanUserFileVOList = parentIdFileListMap.get(context.getParentId());
        if (CollectionUtil.isEmpty(rPanUserFileVOList)) {
            return Lists.newArrayList();
        }

        return rPanUserFileVOList;
    }

    /**
     * 转存到我的网盘
     * <p>
     * 1. 校验分享状态
     * 2. 校验文件ID是否合法
     * 3. 执行保存我的网盘动作
     *
     * @param context
     */
    @Override
    public void saveFiles(ShareSaveContext context) {
        checkShareStatus(context.getShareId());
        checkFileIdIsOnShareStatus(context.getShareId(), context.getFileIdList());
        doSaveFiles(context);
    }

    /**
     * 分享的文件下载
     * <p>
     * 1. 校验分享状态
     * 2. 校验文件ID的合法性
     * 3. 执行文件下载的动作
     *
     * @param context
     */
    @Override
    public void download(ShareFileDownloadContext context) {
        checkShareStatus(context.getShareId());
        checkFileIdIsOnShareStatus(context.getShareId(), Lists.newArrayList(context.getFileId()));
        doDownload(context);
    }

    /**
     * 刷新受影响的对应的分享的状态
     * <p>
     * 1. 查询所有受影响的分享的ID的集合
     * 2. 去判断每个分享对应的文件以及所有的父文件信息均为正常，把分享状态设置为正常
     * 3. 如果有分享的文件或者父文件信息被删除，变更该分享状态为有文件被删除
     *
     * @param allAvailableFileIdList
     */
    @Override
    public void refreshShareStatus(List<Long> allAvailableFileIdList) {
        List<Long> shareIdList = getShareIdListByFileIdList(allAvailableFileIdList);
        if (CollectionUtils.isEmpty(shareIdList)) {
            return;
        }

        Set<Long> shareIdSet = Sets.newHashSet(shareIdList);
        shareIdSet.stream().forEach(this::refreshOneShareStatus);
    }

    /**
     * 刷新一个分享的分享状态
     * <p>
     * 1. 查询对应的分享信息，判断有效
     * 2. 去判断每个分享对应的文件以及所有的父文件信息均为正常，把分享状态设置为正常
     * 3. 如果有父文件被删除，设置为删除状态
     *
     * @param shareId
     */
    private void refreshOneShareStatus(Long shareId) {
        RPanShare record = getById(shareId);
        if (Objects.isNull(record)) {
            return;
        }

        ShareStatusEnum shareStatus = ShareStatusEnum.NORMAL;
        if (!checkShareFileAvailable(shareId)) {
            shareStatus = ShareStatusEnum.FILE_DELETED;
        }

        // 如果一致，直接忽略
        if (Objects.equals(record.getShareStatus(), shareStatus.getCode())) {
            return;
        }

        doChangeShareStatus(shareId, shareStatus);
    }

    /**
     * 执行刷新文件分享状态的动作
     *
     * @param shareId
     * @param shareStatus
     */
    private void doChangeShareStatus(Long shareId, ShareStatusEnum shareStatus) {
        LambdaUpdateWrapper<RPanShare> lambdaUpdateWrapper = Wrappers.<RPanShare>lambdaUpdate()
                .eq(RPanShare::getShareId, shareId)
                .set(RPanShare::getShareStatus, shareStatus.getCode());
        if (!update(lambdaUpdateWrapper)){
            applicationContext.publishEvent(new ErrorLogEvent(this,
                    "更新分享状态失败，请手动更改状态，分享ID为："+shareId+"，分享状态改为："+shareStatus.getCode(),
                    RPanConstants.ZERO_LONG));
        }

    }

    /**
     * 检查该分享所有的文件以及所有的父文件均为正常状态
     *
     * @param shareId
     * @return
     */
    private boolean checkShareFileAvailable(Long shareId) {
        List<Long> shareFileIdList = getShareFileIdList(shareId);
        for (Long fileId : shareFileIdList) {
            if (!checkUpFileAvailable(fileId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查该文件以及所有的文件夹信息均为正常状态
     *
     * @param fileId
     * @return
     */
    private boolean checkUpFileAvailable(Long fileId) {
        RPanUserFile record = iUserFileService.getById(fileId);
        if (Objects.isNull(record)) {
            return false;
        }
        if (Objects.equals(record.getDelFlag(), DelFlagEnum.YES.getCode())) {
            return false;
        }
        if (Objects.equals(record.getParentId(), FileConstants.TOP_PARENT_ID)) {
            return true;
        }
        return checkUpFileAvailable(record.getParentId());
    }

    /**
     * 通过文件ID查询对应的分享ID集合
     *
     * @param allAvailableFileIdList
     * @return
     */
    private List<Long> getShareIdListByFileIdList(List<Long> allAvailableFileIdList) {
        LambdaQueryWrapper<RPanShareFile> lambdaQueryWrapper = Wrappers.<RPanShareFile>lambdaQuery()
                .select(RPanShareFile::getShareId)
                .in(RPanShareFile::getFileId, allAvailableFileIdList);
        List<Long> shareIdList = iShareFileService.listObjs(lambdaQueryWrapper, value -> (Long) value);
        return shareIdList;
    }

    /**
     * 执行分享文件下载动作
     * 委托文件模块进行执行
     *
     * @param context
     */
    private void doDownload(ShareFileDownloadContext context) {
        FileDownloadContext fileDownloadContext = new FileDownloadContext();
        fileDownloadContext.setFileId(context.getFileId());
        fileDownloadContext.setUserId(context.getUserId());
        fileDownloadContext.setResponse(context.getResponse());
        iUserFileService.downloadWithoutCheckUser(fileDownloadContext);
    }

    /**
     * 执行保存到我的网盘动作
     * 委托文件模块做文件拷贝的动作
     *
     * @param context
     */
    private void doSaveFiles(ShareSaveContext context) {
        CopyFileContext copyFileContext = new CopyFileContext();
        copyFileContext.setFileIdList(context.getFileIdList());
        copyFileContext.setTargetParentId(context.getTargetParentId());
        copyFileContext.setUserId(context.getUserId());

        iUserFileService.copy(copyFileContext);
    }

    /**
     * 校验文件ID是否属于某一个分享
     *
     * @param shareId
     * @param fileIdList
     */
    private void checkFileIdIsOnShareStatus(Long shareId, List<Long> fileIdList) {
        checkFileIdIsOnShareStatusAndGetAllShareUserFiles(shareId, fileIdList);
    }

    /**
     * 校验文件是否处于分享状态，返回该分享的所有文件列表
     *
     * @param shareId
     * @param fileIdList
     * @return
     */
    private List<RPanUserFileVO> checkFileIdIsOnShareStatusAndGetAllShareUserFiles(Long shareId, List<Long> fileIdList) {
        List<Long> shareFileIdList = getShareFileIdList(shareId);
        if (CollectionUtil.isEmpty(shareFileIdList)) {
            return Lists.newArrayList();
        }

        List<RPanUserFile> allFileRecords = iUserFileService.findAllFileRecordsByFileIdList(shareFileIdList);
        if (CollectionUtil.isEmpty(allFileRecords)) {
            return Lists.newArrayList();
        }
        // 查询出来的子文件，有可能存在被删除的文件的信息
        allFileRecords = allFileRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> ObjectUtil.equal(record.getDelFlag(), DelFlagEnum.NO.getCode()))
                .collect(Collectors.toList());

        List<Long> allFileIdList = allFileRecords.stream()
                .map(RPanUserFile::getFileId)
                .collect(Collectors.toList());
        if (allFileIdList.containsAll(fileIdList)) {
            return iUserFileService.transferVOList(allFileRecords);
        }

        throw new RPanBusinessException(ResponseCode.SHARE_FILE_MISS);
    }

    /**
     * 查询分享对应的文件ID集合
     *
     * @param shareId
     * @return
     */
    private List<Long> getShareFileIdList(Long shareId) {
        if (ObjectUtil.isNull(shareId)) {
            return Lists.newArrayList();
        }

        LambdaQueryWrapper<RPanShareFile> lambdaQueryWrapper = Wrappers.<RPanShareFile>lambdaQuery()
                .select(RPanShareFile::getFileId)
                .eq(RPanShareFile::getShareId, shareId);

        List<Long> fileIdList = iShareFileService.listObjs(lambdaQueryWrapper, value -> (long) value);

        return fileIdList;
    }

    /**
     * 填充简单分享详情的用户信息
     * 这里的实现和填充分享详情是一摸一样的，但是为了方便以后业务的扩展，这里单独写成一个方法，不进行复用
     *
     * @param context
     */
    private void assembleShareSimpleUserInfo(QueryShareSimpleDetailContext context) {
        RPanUser record = iUserService.getById(context.getRecord().getCreateUser());
        if (ObjectUtil.isNull(record)) {
            throw new RPanBusinessException("用户信息查询失败");
        }

        ShareUserInfoVO shareUserInfoVO = new ShareUserInfoVO();
        shareUserInfoVO.setUserId(record.getUserId());
        shareUserInfoVO.setUsername(encryptUsername(record.getUsername()));

        context.getVo().setShareUserInfoVO(shareUserInfoVO);
    }

    /**
     * 填充简单分享详情实体信息
     *
     * @param context
     */
    private void assembleMainShareSimpleInfo(QueryShareSimpleDetailContext context) {
        RPanShare record = context.getRecord();
        ShareSimpleDetailVO vo = context.getVo();
        vo.setShareId(record.getShareId());
        vo.setShareName(record.getShareName());
    }

    /**
     * 初始化简单分享详情的VO对象
     *
     * @param context
     */
    private void initShareSimpleVO(QueryShareSimpleDetailContext context) {
        ShareSimpleDetailVO shareSimpleDetailVO = new ShareSimpleDetailVO();
        context.setVo(shareSimpleDetailVO);
    }

    /**
     * 拼装查询分享者的信息
     *
     * @param context
     */
    private void assembleShareUserInfo(QueryShareDetailContext context) {
        RPanUser record = iUserService.getById(context.getRecord().getCreateUser());
        if (ObjectUtil.isNull(record)) {
            throw new RPanBusinessException("用户查询信息失败");
        }

        ShareUserInfoVO shareUserInfoVO = new ShareUserInfoVO();
        shareUserInfoVO.setUserId(record.getUserId());
        shareUserInfoVO.setUsername(encryptUsername(record.getUsername()));
        context.getVo().setShareUserInfoVO(shareUserInfoVO);
    }

    /**
     * 加密用户名称 username -> ****
     *
     * @param username
     * @return
     */
    private String encryptUsername(String username) {
        // StringBuffer 相比StringBuilder 是线程安全的
        StringBuffer stringBuffer = new StringBuffer(username);
        stringBuffer.replace(RPanConstants.TWO_INT, username.length() - RPanConstants.TWO_INT, RPanConstants.COMMON_ENCRYPT_STR);

        return stringBuffer.toString();
    }

    /**
     * 查询分享对应的文件列表
     * <p>
     * 1. 查询分享对应的文件ID集合
     * 2. 根据文件ID来查询文件列表信息
     *
     * @param context
     */
    private void assembleShareFilesInfo(QueryShareDetailContext context) {

        List<Long> fileIdList = getShareFileIdList(context.getShareId());

        QueryFileListContext queryFileListContext = new QueryFileListContext();
        queryFileListContext.setUserId(context.getRecord().getCreateUser());
        queryFileListContext.setDelFlag(DelFlagEnum.NO.getCode());
        queryFileListContext.setFileIdList(fileIdList);

        List<RPanUserFileVO> rPanUserFileVOList = iUserFileService.getFileList(queryFileListContext);
        context.getVo().setRPanUserFileVOList(rPanUserFileVOList);
    }

    /**
     * 查询分享的主题信息
     *
     * @param context
     */
    private void assembleMainShareInfo(QueryShareDetailContext context) {
        RPanShare record = context.getRecord();

        ShareDetailVO vo = context.getVo();
        vo.setShareId(record.getShareId());
        vo.setShareName(record.getShareName());
        vo.setCreateTime(record.getCreateTime());
        vo.setShareDay(record.getShareDay());
        vo.setShareEndTime(record.getShareEndTime());
    }

    /**
     * 初始化文件详情VO实体
     *
     * @param context
     */
    private void initShareVO(QueryShareDetailContext context) {
        ShareDetailVO vo = new ShareDetailVO();
        context.setVo(vo);
    }

    /**
     * 生成一个短期分享token
     *
     * @param context
     * @return
     */
    private String generateShareToken(CheckShareCodeContext context) {
        RPanShare record = context.getRecord();

        String token = JwtUtil.generateToken(UUIDUtil.getUUID(),
                ShareConstants.SHARE_ID,
                record.getShareId(),
                ShareConstants.ONE_HOUR_LONG);

        return token;
    }

    /**
     * 校验分享码是否正确
     *
     * @param context
     */
    private void doCheckShareCode(CheckShareCodeContext context) {
        RPanShare record = context.getRecord();
        if (ObjectUtil.notEqual(context.getShareCode(), record.getShareCode())) {
            throw new RPanBusinessException("分享码错误");
        }
    }

    /**
     * 检查分享的状态是否正常
     *
     * @param shareId
     * @return
     */
    private RPanShare checkShareStatus(Long shareId) {

        RPanShare record = getById(shareId);

        if (Objects.isNull(record)) {
            throw new RPanBusinessException(ResponseCode.SHARE_CANCELLED);
        }

        if (Objects.equals(ShareStatusEnum.FILE_DELETED.getCode(), record.getShareStatus())) {
            throw new RPanBusinessException(ResponseCode.SHARE_FILE_MISS);
        }

        if (Objects.equals(ShareDayTypeEnum.PERMANENT_VALIDITY.getCode(), record.getShareDayType())) {
            return record;
        }

        if (record.getShareEndTime().before(new Date())) {
            throw new RPanBusinessException(ResponseCode.SHARE_EXPIRE);
        }

        return record;
    }

    /**
     * 取消文件和分享的关联关系数据
     *
     * @param context
     */
    private void doCancelShareFiles(CancelShareContext context) {
        LambdaQueryWrapper<RPanShareFile> lambdaQueryWrapper = Wrappers.<RPanShareFile>lambdaQuery()
                .in(RPanShareFile::getShareId, context.getShareIdList())
                .eq(RPanShareFile::getCreateUser, context.getUserId());

        if (!iShareFileService.remove(lambdaQueryWrapper)) {
            throw new RPanBusinessException("取消分享失败");
        }
    }

    /**
     * 执行取消文件分享的动作
     *
     * @param context
     */
    private void doCancelShare(CancelShareContext context) {
        List<Long> shareIdList = context.getShareIdList();

        if (!removeByIds(shareIdList)) {
            throw new RPanBusinessException("取消分享失败");
        }
    }

    /**
     * 检查用户是否拥有取消对应分享链接的权限
     *
     * @param context
     */
    private void checkUserCancelSharePermission(CancelShareContext context) {
        List<Long> shareIdList = context.getShareIdList();
        Long userId = context.getUserId();
        List<RPanShare> records = listByIds(shareIdList);

        if (ObjectUtil.isEmpty(records)) {
            throw new RPanBusinessException("您无权操作取消分享的动作");
        }

        for (RPanShare record : records) {
            if (ObjectUtil.notEqual(record.getCreateUser(), userId)) {
                throw new RPanBusinessException("您无权操作取消分享的动作");
            }
        }
    }

    private void afterCreate(CreateShareUrlContext context, RPanShareUrlVO vo) {

    }

    /**
     * 拼装对应的返回VO
     *
     * @param context
     * @return
     */
    private RPanShareUrlVO assembleShareVO(CreateShareUrlContext context) {
        RPanShare record = context.getRecord();
        RPanShareUrlVO vo = new RPanShareUrlVO();
        vo.setShareId(record.getShareId());
        vo.setShareName(record.getShareName());
        vo.setShareUrl(record.getShareUrl());
        vo.setShareCode(record.getShareCode());
        vo.setShareStatus(record.getShareStatus());

        return vo;
    }

    /**
     * 保存分享和分享文件的关联关系
     *
     * @param context
     */
    private void saveShareFiles(CreateShareUrlContext context) {
        SaveShareFilesContext saveShareFilesContext = new SaveShareFilesContext();
        saveShareFilesContext.setShareId(context.getRecord().getShareId());
        saveShareFilesContext.setShareFileIdList(context.getShareFileIdList());
        saveShareFilesContext.setUserId(context.getUserId());
        iShareFileService.saveShareFiles(saveShareFilesContext);
    }

    /**
     * 创建拼装分享实体保存到数据库
     *
     * @param context
     */
    private void saveShare(CreateShareUrlContext context) {
        RPanShare record = new RPanShare();

        record.setShareId(IdUtil.get());
        record.setShareName(context.getShareName());
        record.setShareType(context.getShareType());
        record.setShareDayType(context.getShareDayType());

        Integer shareDay = ShareDayTypeEnum.getShareDayByCode(context.getShareDayType());
        if (Objects.equals(RPanConstants.MINUS_ONE_INT, shareDay)) {
            throw new RPanBusinessException("分享天数非法");
        }

        record.setShareDay(shareDay);
        record.setShareEndTime(DateUtil.offsetDay(new Date(), shareDay));
        record.setShareUrl(createShareUrl(record.getShareId()));
        record.setShareCode(createShareCode());
        record.setShareStatus(ShareStatusEnum.NORMAL.getCode());
        record.setCreateUser(context.getUserId());
        record.setCreateTime(new Date());

        if (!save(record)) {
            throw new RPanBusinessException("保存分享信息失败");
        }

        context.setRecord(record);
    }

    /**
     * 创建分享的分享码
     *
     * @return
     */
    private String createShareCode() {
        return RandomStringUtils.randomAlphabetic(4).toLowerCase();
    }

    /**
     * 创建分享的URL
     *
     * @param shareId
     * @return
     */
    private String createShareUrl(Long shareId) {
        if (Objects.isNull(shareId)) {
            throw new RPanBusinessException("分享的ID不能为空");
        }

        String sharePrefix = config.getSharePrefix();
        if (!sharePrefix.endsWith(RPanConstants.SLASH_STR)) {
            sharePrefix += RPanConstants.SLASH_STR;
        }

        return sharePrefix + URLEncoder.encode(IdUtil.encrypt(shareId));
    }
}




