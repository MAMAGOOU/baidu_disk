package com.rocket.pan.server.common.schedule.launcher;

import com.rocket.pan.schedule.ScheduleManager;
import com.rocket.pan.schedule.ScheduleTask;
import com.rocket.pan.server.common.schedule.task.RebuildShareSimpleDetailBloomFilterTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 定时重建简单分享详情布隆过滤器任务触发器
 *
 * @author 19750
 * @version 1.0
 */
@Log4j2
@Component
public class RebuildShareSimpleDetailBloomFilterTaskLauncher implements CommandLineRunner {

    /**
     * CRON表达式，每天的0点0分1秒执行任务
     */
    private final static String CRON = "1 0 0 * * ?";

    @Autowired
    private RebuildShareSimpleDetailBloomFilterTask task;

    @Autowired
    private ScheduleManager scheduleManager;

    @Override
    public void run(String... args) throws Exception {
        scheduleManager.startTask(task, CRON);
    }
}
