package com.rocket.pan.schedule.test;

import com.rocket.pan.schedule.ScheduleManager;
import com.rocket.pan.schedule.test.config.ScheduleTestConfig;
import com.rocket.pan.schedule.test.task.SimpleScheduleTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 定时任务模块单元测试
 *
 * @author 19750
 * @version 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ScheduleTestConfig.class)
public class ScheduleTaskTest {

    @Autowired
    private ScheduleManager manager;

    @Autowired
    private SimpleScheduleTask scheduleTask;

    @Test
    public void testRunScheduleTask() throws Exception {

        String cron = "0/5 * * * * ? ";

        String key = manager.startTask(scheduleTask, cron);
        // 需要让主线程阻塞10s，否则主线程执行完毕后定时任务不会得到执行，并且容器会进行销毁
        Thread.sleep(10000);

        cron = "0/1 * * * * ? ";

        key = manager.changeTask(key, cron);

        Thread.sleep(10000);

        manager.stopTask(key);

    }
}
