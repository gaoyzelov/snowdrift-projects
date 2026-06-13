package com.snowdrift.framework.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 跨域配置属性
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.web.cors")
public class CorsProperties {

    /**
     * 是否启用 CORS 跨域支持，默认关闭
     */
    private boolean enabled = false;

    /**
     * 允许的路径，默认 /**
     */
    private String path = "/**";

    /**
     * 允许的源（支持通配符），默认 *
     */
    private List<String> allowedOriginPatterns = List.of("*");

    /**
     * 允许的请求方法，默认 GET,POST,PUT,DELETE,OPTIONS
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /**
     * 允许的请求头，默认 *
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 是否允许携带凭证（Cookie），默认 true
     */
    private boolean allowCredentials = true;

    /**
     * 预检请求缓存时间（秒），默认 3600
     */
    private long maxAge = 3600;
}
