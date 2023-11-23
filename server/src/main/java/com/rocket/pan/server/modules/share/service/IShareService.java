package com.rocket.pan.server.modules.share.service;

import com.rocket.pan.server.modules.file.vo.RPanUserFileVO;
import com.rocket.pan.server.modules.share.context.*;
import com.rocket.pan.server.modules.share.entity.RPanShare;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rocket.pan.server.modules.vo.RPanShareUrlListVO;
import com.rocket.pan.server.modules.vo.RPanShareUrlVO;
import com.rocket.pan.server.modules.vo.ShareDetailVO;
import com.rocket.pan.server.modules.vo.ShareSimpleDetailVO;

import java.util.List;

/**
 * @author 19750
 * @description 针对表【r_pan_share(用户分享表)】的数据库操作Service
 * @createDate 2023-11-11 14:45:54
 */
public interface IShareService extends IService<RPanShare> {

    /**
     * 创建分享链接
     *
     * @param context
     * @return
     */
    RPanShareUrlVO create(CreateShareUrlContext context);

    /**
     * 查询用户分享列表
     *
     * @param context
     * @return
     */
    List<RPanShareUrlListVO> getShares(QueryShareListContext context);

    /**
     * 取消分享链接
     *
     * @param context
     */
    void cancelShare(CancelShareContext context);

    /**
     * 校验分享码
     *
     * @param context
     * @return
     */
    String checkShareCode(CheckShareCodeContext context);

    /**
     * 查询分享详情
     *
     * @param context
     * @return
     */
    ShareDetailVO detail(QueryShareDetailContext context);

    /**
     * 查询分享的简单详情
     *
     * @param context
     * @return
     */
    ShareSimpleDetailVO simpleDetail(QueryShareSimpleDetailContext context);

    /**
     * 获取下一级的文件列表
     *
     * @param context
     * @return
     */
    List<RPanUserFileVO> fileList(QueryChildFileListContext context);

    /**
     * 转存到我的网盘
     * <p>
     *
     * @param context
     */
    void saveFiles(ShareSaveContext context);

    /**
     * 分享的文件下载
     *
     * @param context
     */
    void download(ShareFileDownloadContext context);

    /**
     * 刷新受影响的对应的分享的状态
     *
     * @param allAvailableFileIdList
     */
    void refreshShareStatus(List<Long> allAvailableFileIdList);
}
