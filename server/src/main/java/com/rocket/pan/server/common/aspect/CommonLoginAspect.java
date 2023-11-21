package com.rocket.pan.server.common.aspect;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.rocket.pan.cache.core.constants.CacheConstants;
import com.rocket.pan.core.response.R;
import com.rocket.pan.core.response.ResponseCode;
import com.rocket.pan.server.common.annotation.LoginIgnore;
import com.rocket.pan.server.common.utils.UserIdUtil;
import com.rocket.pan.server.modules.user.constants.UserConstants;
import com.rocket.pan.core.utils.JwtUtil;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 统一的登录拦截校验切面逻辑实现类
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
@Aspect
public class CommonLoginAspect {
    /**
     * 登录认证参数名称
     */
    private static final String LOGIN_AUTH_PARAM_NAME = "authorization";

    /**
     * 请求头登录认证key
     */
    private static final String LOGIN_AUTH_REQUEST_HEADER_NAME = "Authorization";

    /**
     * 切点表达式
     */
    private static final String POINT_CUT = "execution(* com.rocket.pan.server.modules.*.controller..*(..))";


    @Autowired
    private CacheManager cacheManager;

    @Pointcut(POINT_CUT)
    public void loginAuth() {

    }

    /**
     * 切点的环绕增强逻辑
     * 1. 判断是否需要校验登录信息
     * 2. 校验登录信息：a. 从请求中获取或参数中获取token, b. 从缓存中获取token，进行比对，c. 将解析的userId存入threadLocal，供下游使用
     *
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("loginAuth()")
    public Object loginAuthAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (checkNeedCheckLoginInfo(proceedingJoinPoint)) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String requestURI = request.getRequestURI();
            log.info("成功拦截到请求，URI为：{}", requestURI);
            if (!checkAndSaveUserId(request)) {
                log.warn("成功拦截到请求，URI为：{}，检测到用户未登录，将跳转到登录页面", requestURI);
                return R.fail(ResponseCode.NEED_LOGIN);
            }
            log.info("成功拦截到请求，URI：{}，请求通过", requestURI);
        }
        return proceedingJoinPoint.proceed();
    }

    /**
     * 校验token并提取userId
     *
     * @param request
     * @return
     */
    private boolean checkAndSaveUserId(HttpServletRequest request) {
        String accessToken = request.getHeader(LOGIN_AUTH_REQUEST_HEADER_NAME);
        if (StrUtil.isBlank(accessToken)) {
            accessToken = request.getParameter(LOGIN_AUTH_PARAM_NAME);
        }

        if (StrUtil.isBlank(accessToken)) {
            return false;
        }

        Object userId = JwtUtil.analyzeToken(accessToken, UserConstants.LOGIN_USER_ID);
        if (ObjectUtil.isNull(userId)) {
            return false;
        }

        Cache cache = cacheManager.getCache(CacheConstants.R_PAN_CACHE_NAME);
        String redisAccessToken = cache.get(UserConstants.USER_LOGIN_PREFIX + userId, String.class);
        if (ObjectUtil.isNull(redisAccessToken)) {
            return false;
        }

        if (ObjectUtil.equal(redisAccessToken, accessToken)) {
            saveUserId(userId);
            return true;
        }
        return false;
    }

    /**
     * 保存用户ID到线程上下文中
     *
     * @param userId
     */
    private void saveUserId(Object userId) {
        UserIdUtil.set(Long.valueOf(String.valueOf(userId)));
    }

    /**
     * 校验是否需要校验登录信息
     *
     * @param proceedingJoinPoint
     * @return true 需要校验登录信息，false 不需要校验登录信息
     */
    private boolean checkNeedCheckLoginInfo(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        return !method.isAnnotationPresent(LoginIgnore.class);
    }
}
