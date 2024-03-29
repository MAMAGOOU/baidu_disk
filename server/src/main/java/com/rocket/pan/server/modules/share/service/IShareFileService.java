package com.rocket.pan.server.modules.share.service;

import com.rocket.pan.server.modules.share.context.SaveShareFilesContext;
import com.rocket.pan.server.modules.share.entity.RPanShareFile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 19750
 * @description 针对表【r_pan_share_file(用户分享文件表)】的数据库操作Service
 * @createDate 2023-11-11 14:45:54
 */
public interface IShareFileService extends IService<RPanShareFile> {

    /**
     * 保存分享文件的对应关系
     *
     * @param saveShareFilesContext
     */
    void saveShareFiles(SaveShareFilesContext saveShareFilesContext);

}
