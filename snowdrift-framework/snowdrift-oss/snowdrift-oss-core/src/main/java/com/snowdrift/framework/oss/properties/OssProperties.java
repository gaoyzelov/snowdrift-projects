package com.snowdrift.framework.oss.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * OSS 配置属性
 * <p>
 * Spring Boot 配置属性类，用于读取 application.yml 中的 OSS 配置
 * 启用 @Valid 支持配置校验，启动时自动验证 OssInstanceProperties
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description Spring Boot 配置属性类，用于读取 application.yml 中的 OSS 配置
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.oss")
public class OssProperties {
    
    /**
     * 默认配置标识
     */
    @NotBlank(message = "默认配置标识不能为空")
    private String defaultConfigKey = "default";
    
    /**
     * 多配置映射（支持多个 OSS 配置）
     * Key: 配置标识（如：default、backup）
     * Value: 配置详情
     * <p>
     * 使用 @Valid 启用嵌套校验，确保每个 OssInstanceProperties 都被验证
     */
    @Valid
    private Map<String, OssInstanceProperties> configs = new HashMap<>();

}
