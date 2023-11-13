package com.rocket.pan.server;

import com.rocket.pan.constants.RPanConstants;
import com.rocket.pan.response.R;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

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
