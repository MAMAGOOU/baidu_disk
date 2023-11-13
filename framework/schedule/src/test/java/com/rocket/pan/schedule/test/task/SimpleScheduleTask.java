package com.rocket.pan.schedule.test.task;

import com.rocket.pan.schedule.ScheduleTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
public class SimpleScheduleTask implements ScheduleTask {
    /**
     * 获取定时任务名称
     *
     * @return 定时任务名称
     */
    @Override
    public String getName() {
        return "测试定时任务";
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        log.info(getName()+"正在执行。。。");
    }
}
