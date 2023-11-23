package com.rocket.pan.server.common.listener.test;

import com.rocket.pan.server.common.event.test.TestEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 测试事件处理器
 *
 * @author 19750
 * @version 1.0
 */
@Component
@Log4j2
public class TestEventListener {
    /**
     * 监听测试事件
     *
     * @param event
     * @throws InterruptedException
     */

    @EventListener(TestEvent.class)
    @Async(value = "eventListenerTaskExecutor")
    public void test(TestEvent event) throws InterruptedException {
        System.out.println(event+"接收到事件===========》");
        Thread.sleep(2000);
        log.info("TestEventListener start process, th thread name is {}",
                Thread.currentThread().getName());
    }
}
