package com.rocket.pan.server.common.annotation;

import java.lang.annotation.*;

/**
 * 该注解主要影响哪些不需要登录的接口
 * 标记该注解的方法会自动屏蔽统一的登录拦截校验逻辑
 *
 * @author 19750
 * @version 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LoginIgnore {
}
