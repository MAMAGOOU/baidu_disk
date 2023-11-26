package com.rocket.pan.lock.core.key;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.rocket.pan.lock.core.LockContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 标准的key生成器
 *
 * @author 19750
 * @version 1.0
 */
@Component
public class StandardKeyGenerator extends AbstractKeyGenerator {

    /**
     * 标准key的生成方法
     * 生成格式：className:methodName:parameterType1:...:value1:value2:...
     *
     * @param lockContext
     * @param keyValueMap
     * @return
     */
    @Override
    protected String doGenerateKey(LockContext lockContext, Map<String, String> keyValueMap) {
        ArrayList<Object> keyList = Lists.newArrayList();
        keyList.add(lockContext.getClassName());
        keyList.add(lockContext.getMethodName());

        Class[] parameterTypes = lockContext.getParameterTypes();
        if (ArrayUtils.isNotEmpty(parameterTypes)) {
            Arrays.stream(parameterTypes).forEach(parameterType -> {
                keyList.add(parameterType.toString());
            });
        } else {
            keyList.add(Void.class.toString());
        }

        Collection<String> values = keyValueMap.values();
        if (CollectionUtils.isNotEmpty(values)) {
            values.stream().forEach(value -> keyList.add(value));
        }

        return Joiner.on(",").join(keyList);
    }
}
