package com.rocket.pan.schedule.test.config;

import com.rocket.pan.core.constants.RPanConstants;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 单元测试配置类
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
@ComponentScan(RPanConstants.BASE_COMPONENT_SCAN_PATH + ".schedule")
public class ScheduleTestConfig {
}
