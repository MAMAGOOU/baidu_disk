package com.rocket.pan.server.modules.recycle.service;

import com.rocket.pan.server.modules.file.vo.RPanUserFileVO;
import com.rocket.pan.server.modules.recycle.context.DeleteContext;
import com.rocket.pan.server.modules.recycle.context.QueryRecycleFileListContext;
import com.rocket.pan.server.modules.recycle.context.RestoreContext;

import java.util.List;

public interface IRecycleService {
    /**
     * 查询用户回收站文件列表
     *
     * @param context
     * @return
     */
    List<RPanUserFileVO> recycles(QueryRecycleFileListContext context);

    /**
     * 文件还原
     *
     * @param context
     */
    void restore(RestoreContext context);

    /**
     * 文件彻底删除
     *
     * @param context
     */
    void delete(DeleteContext context);
}
