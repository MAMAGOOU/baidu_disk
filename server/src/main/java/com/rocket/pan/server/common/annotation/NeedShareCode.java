package com.rocket.pan.server.common.annotation;

import java.lang.annotation.*;

/**
 * 该注解主要影响需要分享码校验的接口
 * @author 19750
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface NeedShareCode {

}
