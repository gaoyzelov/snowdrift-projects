package com.snowdrift.framework.cache.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 缓存配置属性
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.cache")
public class CacheProperties {

    /**
     * 是否启用安全模块
     */
    @NotNull
    private boolean enabled = true;

    /**
     * 缓存类型（caffeine / redis / redisson），不配置则根据类路径自动检测
     */
    private CacheTypeEnum type;

    /**
     * key 全局前缀
     */
    private String keyPrefix;

    /**
     * 全局默认 TTL
     */
    private Duration keyTtl = Duration.ofMinutes(30);

    /**
     * 最大缓存条目数
     * 仅当缓存类型为 caffeine 时有效
     */
    private long maxSize = 10000;
}
