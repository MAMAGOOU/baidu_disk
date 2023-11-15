package com.rocket.pan.server.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.rocket.pan.core.constants.RPanConstants;

/**
 * 存储用户ID工具类
 *
 * @author 19750
 * @version 1.0
 */
public class UserIdUtil {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void set(Long userId) {
        threadLocal.set(userId);
    }

    public static Long get() {
        Long userId = threadLocal.get();
        if (ObjectUtil.isNull(userId)) {
            return RPanConstants.ZERO_LONG;
        }
        return userId;
    }

    public static void remove() {
        threadLocal.remove();
    }
}
