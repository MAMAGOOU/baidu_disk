package com.rocket.pan.server.modules.test.controller;

import com.rocket.pan.core.response.R;
import com.rocket.pan.server.common.annotation.LoginIgnore;
import com.rocket.pan.server.common.event.test.TestEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试处理器
 *
 * @author 19750
 * @version 1.0
 */
@RestController
public class TestController implements ApplicationContextAware {
    private ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 测试事件发布
     *
     * @return
     */
    @GetMapping("/test")
    @LoginIgnore
    public R test() {
        applicationContext.publishEvent(new TestEvent(this, "test"));
        return R.success();
    }

}
