package com.rocket.pan.schedule;

/**
 * @author 19750
 * @version 1.0
 */
public interface ScheduleTask extends Runnable {
    /**
     * 获取定时任务名称
     *
     * @return 定时任务名称
     */
    String getName();
}
