package com.snowdrift.framework.oss.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * OSS 配置属性
 *
 * @author 83674
 * @date 2026/5/9
 * @description Spring Boot 配置属性类，用于读取 application.yml 中的 OSS 配置
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.oss")
public class OssProperties {
    
    /**
     * 默认配置标识
     */
    private String defaultConfigKey = "default";
    
    /**
     * 多配置映射（支持多个 OSS 配置）
     * Key: 配置标识（如：default、backup）
     * Value: 配置详情
     */
    private Map<String, OssInstanceProperties> configs = new HashMap<>();

}
