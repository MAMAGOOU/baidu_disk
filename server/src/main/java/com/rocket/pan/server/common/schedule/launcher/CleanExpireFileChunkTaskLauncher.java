package com.rocket.pan.server.common.schedule.launcher;

import com.rocket.pan.schedule.ScheduleManager;
import com.rocket.pan.server.common.schedule.task.CleanExpireChunkFileTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
public class CleanExpireFileChunkTaskLauncher implements CommandLineRunner {
    // private final static String CRON = "1 0 0 * * ? ";
    private final static String CRON = "0/5 * * * * ? ";

    @Autowired
    private CleanExpireChunkFileTask task;

    @Autowired
    private ScheduleManager scheduleManager;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("===【开始执行了】===");
        scheduleManager.startTask(task, CRON);
    }
}
