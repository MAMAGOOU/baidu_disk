package com.rocket.pan.lock.core;

import com.rocket.pan.lock.core.annotation.Lock;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 锁实体的上下文信息
 * 主要是做切点的实体解析，为整体逻辑公用
 * 定义公用的属性
 *
 * @author 19750
 * @version 1.0
 */
@Data
public class LockContext {
    /**
     * 切点方法所属类的名称
     */
    private String className;

    /**
     * 切点方法的名称
     */
    private String methodName;

    /**
     * 切点方法上标记的锁注解
     */
    private Lock annotation;
    /**
     * 类的class对象
     */
    private Class classType;
    /**
     * 当前调用的方法的实体
     */
    private Method method;
    /**
     * 参数列表实体
     */
    private Object[] args;
    /**
     * 参数列表类型
     */
    private Class[] parameterTypes;

    /**
     * 代理对象实体
     */
    private Object target;

    /**
     * 初始化实体对象
     *
     * @param proceedingJoinPoint
     * @return
     */
    public static LockContext init(ProceedingJoinPoint proceedingJoinPoint) {
        LockContext lockContext = new LockContext();
        doInit(lockContext, proceedingJoinPoint);
        return lockContext;
    }

    /**
     * 执行初始化的动作
     * 逻辑不复杂，就是做一些属性的填充
     *
     * @param lockContext
     * @param proceedingJoinPoint
     */
    private static void doInit(LockContext lockContext, ProceedingJoinPoint proceedingJoinPoint) {
        Signature signature = proceedingJoinPoint.getSignature();
        Object[] args = proceedingJoinPoint.getArgs();
        Object target = proceedingJoinPoint.getTarget();
        String methodName = signature.getName();
        Class classType = signature.getDeclaringType();
        String className = signature.getDeclaringTypeName();
        MethodSignature methodSignature = (MethodSignature) signature;
        Class[] parameterTypes = methodSignature.getParameterTypes();
        Method method = methodSignature.getMethod();
        Lock annotation= method.getAnnotation(Lock.class);

        lockContext.setArgs(args);
        lockContext.setTarget(target);
        lockContext.setMethodName(methodName);
        lockContext.setClassType(classType);
        lockContext.setClassName(className);
        lockContext.setParameterTypes(parameterTypes);
        lockContext.setMethod(method);
        lockContext.setAnnotation(annotation);
    }
}
