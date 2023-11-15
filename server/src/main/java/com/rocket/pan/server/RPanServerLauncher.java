package com.rocket.pan.server;

import com.rocket.pan.core.constants.RPanConstants;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 19750
 * @version 1.0
 */
@SpringBootApplication(scanBasePackages = RPanConstants.BASE_COMPONENT_SCAN_PATH)
@ServletComponentScan(basePackages = RPanConstants.BASE_COMPONENT_SCAN_PATH)
@EnableTransactionManagement
@MapperScan(basePackages = RPanConstants.BASE_COMPONENT_SCAN_PATH + ".server.modules.**.mapper")
public class RPanServerLauncher {
    public static void main(String[] args) {
        SpringApplication.run(RPanServerLauncher.class);
    }

}
