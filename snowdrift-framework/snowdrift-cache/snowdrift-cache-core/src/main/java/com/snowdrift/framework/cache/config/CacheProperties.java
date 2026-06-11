package com.snowdrift.framework.cache.config;

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
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * Caffeine 专属配置
     */
    private CaffeineConfig caffeine = new CaffeineConfig();

    @Data
    public static class CaffeineConfig {

        /**
         * 最大缓存条目数
         */
        private long maxSize = 10000;

        /**
         * 默认过期时间
         */
        private Duration ttl = Duration.ofMinutes(5);
    }
}
