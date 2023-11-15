package com.rocket.pan.swagger2;

import com.rocket.pan.core.constants.RPanConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * swagger2配置属性实体
 *
 * @author 19750
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "swagger2")
public class Swagger2ConfigProperties {
    private boolean show = true;

    private String groupName = "r-pan";

    private String basePackage = RPanConstants.BASE_COMPONENT_SCAN_PATH;

    private String title = "r-pan-server";

    private String description = "r-pan-server";

    private String termsOfServiceUrl = "http://127.0.0.1:${server.port}";

    private String contactName = "dong";

    private String contactUrl = "no";

    private String contactEmail = "1975015544@qq.com";

    private String version = "1.0";

}
