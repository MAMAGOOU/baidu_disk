package com.rocket.pan.lock.core.key;

import com.google.common.collect.Maps;
import com.rocket.pan.core.utils.SpElUtil;
import com.rocket.pan.lock.core.LockContext;
import com.rocket.pan.lock.core.annotation.Lock;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * 锁的key生成器的公用父类
 *
 * @author 19750
 * @version 1.0
 */
public abstract class AbstractKeyGenerator implements KeyGenerator {
    /**
     * 生成锁的key
     *
     * @param lockContext
     * @return
     */
    @Override
    public String generateKey(LockContext lockContext) {
        Lock annotation = lockContext.getAnnotation();
        String[] keys = annotation.keys();
        Map<String, String> keyValueMap = Maps.newHashMap();
        if (ArrayUtils.isNotEmpty(keys)) {
            Arrays.stream(keys).forEach(key -> {
                keyValueMap.put(key, SpElUtil.getStringValue(key,
                        lockContext.getClassName(),
                        lockContext.getMethodName(),
                        lockContext.getClassType(),
                        lockContext.getMethod(),
                        lockContext.getArgs(),
                        lockContext.getParameterTypes(),
                        lockContext.getTarget()));
            });
        }
        return doGenerateKey(lockContext, keyValueMap);
    }

    /**
     * 具体生成key的执行动作下称到不同的实现类中进行是实现
     * 方便以后采用不同的生成key策略的扩展
     *
     * @param lockContext
     * @param keyValueMap
     * @return
     */
    protected abstract String doGenerateKey(LockContext lockContext, Map<String, String> keyValueMap);
}
