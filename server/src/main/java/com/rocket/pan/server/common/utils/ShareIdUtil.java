package com.rocket.pan.server.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.rocket.pan.core.constants.RPanConstants;

/**
 * 分享ID存储工具类
 *
 * @author 19750
 * @version 1.0
 */
public class ShareIdUtil {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的分享ID
     *
     * @param shareId
     */
    public static void set(Long shareId) {
        threadLocal.set(shareId);
    }

    /**
     * 获取当前线程的分享ID
     *
     * @return
     */
    public static Long get() {
        Long shareId = threadLocal.get();
        if (ObjectUtil.isNull(shareId)) {
            return RPanConstants.ZERO_LONG;
        }
        return shareId;
    }

}
