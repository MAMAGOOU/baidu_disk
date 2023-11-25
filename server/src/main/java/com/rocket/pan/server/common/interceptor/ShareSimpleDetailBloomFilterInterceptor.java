package com.rocket.pan.server.common.interceptor;

import com.rocket.pan.bloom.filter.core.BloomFilter;
import com.rocket.pan.bloom.filter.core.BloomFilterManager;
import com.rocket.pan.core.exception.RPanBusinessException;
import com.rocket.pan.core.response.ResponseCode;
import com.rocket.pan.core.utils.IdUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * 查询简单分享详情布隆过滤器拦截器
 *
 * @author 19750
 * @version 1.0
 */
@Log4j2
@Component
public class ShareSimpleDetailBloomFilterInterceptor implements BloomFilterInterceptor {

    @Autowired
    private BloomFilterManager manager;

    private static final String BLOOM_FILTER_NAME = "SHARE_SIMPLE_DETAIL";
    private static final String INTERCEPTOR_NAME = "ShareSimpleDetailBloomFilterInterceptor";

    /**
     * 拦截器的名称
     *
     * @return
     */
    @Override
    public String getName() {
        return INTERCEPTOR_NAME;
    }

    /**
     * 要拦截的URI的集合
     *
     * @return
     */
    @Override
    public String[] getPathPatterns() {
        return ArrayUtils.toArray("/share/simple");
    }

    /**
     * 要排除拦截的URI的集合
     *
     * @return
     */
    @Override
    public String[] getExcludePatterns() {
        return new String[0];
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String encShareId = request.getParameter("shareId");
        if (StringUtils.isBlank(encShareId)) {
            throw new RPanBusinessException("分享ID不能为空");
        }

        BloomFilter<Long> bloomFilter = manager.getFilter(BLOOM_FILTER_NAME);
        if (Objects.isNull(bloomFilter)) {
            log.info("the bloomFilter named {} is null, give up existence judgment...", BLOOM_FILTER_NAME);
            return true;
        }

        Long shareId = IdUtil.decrypt(encShareId);
        boolean mightContain = bloomFilter.mightContain(shareId);
        if (mightContain) {
            log.info("the bloomFilter named {} judge shareId {} mightContain pass...", BLOOM_FILTER_NAME, shareId);
            return true;
        }

        log.info("the bloomFilter named {} judge shareId {} mightContain fail...", BLOOM_FILTER_NAME, shareId);
        throw new RPanBusinessException(ResponseCode.SHARE_CANCELLED);
    }
}
