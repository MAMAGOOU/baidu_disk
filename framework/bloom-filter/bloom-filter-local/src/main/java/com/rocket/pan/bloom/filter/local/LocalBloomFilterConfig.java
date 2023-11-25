package com.rocket.pan.bloom.filter.local;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 19750
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "com.rocket.pan.bloom.filter.local")
@Data
public class LocalBloomFilterConfig {
    private List<LocalBloomFilterConfigItem> items;
}
