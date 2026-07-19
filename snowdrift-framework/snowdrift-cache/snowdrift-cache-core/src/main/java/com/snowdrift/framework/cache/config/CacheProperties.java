package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.enums.SerializerType;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 缓存配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.cache")
public class CacheProperties {

    /**
     * key 全局前缀
     */
    private String keyPrefix;

    /**
     * 全局默认 TTL
     */
    @NotNull
    private Duration keyTtl = Duration.ofMinutes(30);

    /**
     * 最大缓存条目数
     * 仅当缓存类型为 caffeine 时有效
     */
    @Min(1)
    private long maxSize = 10000;

    /**
     * 序列化器类型，默认 Jackson
     */
    private SerializerType serializer = SerializerType.JACKSON;
}
