package com.rocket.pan.server.common.schedule.task;

import com.rocket.pan.bloom.filter.core.BloomFilter;
import com.rocket.pan.bloom.filter.core.BloomFilterManager;
import com.rocket.pan.schedule.ScheduleTask;
import com.rocket.pan.server.modules.share.service.IShareService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 定时重建简单分享详情布隆过滤器任务
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
public class RebuildShareSimpleDetailBloomFilterTask implements ScheduleTask {
    @Autowired
    private BloomFilterManager manager;

    @Autowired
    private IShareService iShareService;

    private static final String BLOOM_FILTER_NAME = "SHARE_SIMPLE_DETAIL";

    /**
     * 获取定时任务名称
     *
     * @return 定时任务名称
     */
    @Override
    public String getName() {
        return "RebuildShareSimpleDetailBloomFilterTask";
    }

    @Override
    public void run() {
        log.info("start rebuild ShareSimpleDetailBloomFilter...");
        BloomFilter<Long> bloomFilter = manager.getFilter(BLOOM_FILTER_NAME);
        if (Objects.isNull(bloomFilter)) {
            log.info("the bloomFilter named {} is null, give up rebuild...", BLOOM_FILTER_NAME);
            return;
        }
        bloomFilter.clear();

        long startId = 0L;
        long limit = 10000L;
        AtomicLong addCount = new AtomicLong(0L);

        List<Long> shareIdList;

        do {
            shareIdList = iShareService.rollingQueryShareId(startId, limit);
            if (CollectionUtils.isNotEmpty(shareIdList)) {
                shareIdList.stream().forEach(shareId -> {
                    bloomFilter.put(shareId);
                    addCount.incrementAndGet();
                });
                startId = shareIdList.get(shareIdList.size() - 1);
            }
        } while (CollectionUtils.isNotEmpty(shareIdList));

        log.info("finish rebuild ShareSimpleDetailBloomFilter, total set item count {}...", addCount.get());
    }
}
