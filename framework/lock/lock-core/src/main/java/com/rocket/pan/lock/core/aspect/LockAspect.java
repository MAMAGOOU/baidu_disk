package com.rocket.pan.lock.core.aspect;

import cn.hutool.core.util.StrUtil;
import com.rocket.pan.core.exception.RPanFrameworkException;
import com.rocket.pan.lock.core.LockContext;
import com.rocket.pan.lock.core.annotation.Lock;
import com.rocket.pan.lock.core.key.KeyGenerator;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 框架分布式锁统一切面增强逻辑实现
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Aspect
@Log4j2
public class LockAspect implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired
    private LockRegistry lockRegistry;

    /**
     * 定义切点
     */
    @Pointcut(value = "@annotation(com.rocket.pan.lock.core.annotation.Lock)")
    public void lockPointCut() {

    }

    /**
     * 切点增强
     */
    @Around("lockPointCut()")
    public Object aroundLock(ProceedingJoinPoint proceedingJoinPoint) {
        Object result = null;
        LockContext lockContext = LockContext.init(proceedingJoinPoint);
        java.util.concurrent.locks.Lock lock = checkAndGetLock(lockContext);

        if (Objects.isNull(lock)) {
            log.error("lock aspect get lock fail!");
            throw new RPanFrameworkException("aroundLock get lock fail!");
        }

        boolean lockResult = false;
        try {
            lockResult = lock.tryLock(lockContext.getAnnotation().expireSecond(), TimeUnit.SECONDS);
            if (lockResult) {
                Object[] args = proceedingJoinPoint.getArgs();
                result = proceedingJoinPoint.proceed(args);
            }
        } catch (InterruptedException e) {
            log.error("lock aspect tryLock exception.", e);
            throw new RPanFrameworkException("aroundLock tryLock fail.");
        } catch (Throwable e) {
            log.error("lock aspect tryLock exception.", e);
            throw new RPanFrameworkException("aroundLock tryLock fail.");
        } finally {
            // 做资源的释放
            lock.unlock();
        }
        return result;
    }

    /**
     * 检查上下文的配置信息，返回锁实体
     *
     * @param lockContext
     * @return
     */
    private java.util.concurrent.locks.Lock checkAndGetLock(LockContext lockContext) {
        if (Objects.isNull(lockRegistry)) {
            log.error("the lockRegistry is not found ...");
            return null;
        }

        String lockKey = getLockKey(lockContext);
        if (StrUtil.isBlank(lockKey)) {
            return null;
        }

        return lockRegistry.obtain(lockKey);
    }

    /**
     * 获取锁key的私有方法
     *
     * @param lockContext
     * @return
     */
    private String getLockKey(LockContext lockContext) {
        KeyGenerator keyGenerator = applicationContext.getBean(lockContext.getAnnotation().keyGenerator());
        if (Objects.nonNull(keyGenerator)) {
            return keyGenerator.generateKey(lockContext);
        }

        log.error("keyGenerator is not found ...");
        return StrUtil.EMPTY;
    }
}
