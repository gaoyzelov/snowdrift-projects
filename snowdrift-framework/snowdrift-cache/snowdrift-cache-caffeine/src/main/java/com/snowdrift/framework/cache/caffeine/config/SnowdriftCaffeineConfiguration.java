package com.snowdrift.framework.cache.caffeine.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.caffeine.service.SnowdriftCaffeineCacheServiceImpl;
import com.snowdrift.framework.cache.config.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Caffeine 缓存自动配置
 * <p>
 * Caffeine 本地缓存作为最低优先级的后备实现。
 * 仅在容器中不存在其他 {@link ICacheService} Bean（Redis / Redisson）时激活。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(afterName = {
        "com.snowdrift.framework.cache.redis.config.SnowdriftRedisConfiguration",
        "com.snowdrift.framework.cache.redisson.config.SnowdriftRedissonConfiguration"
})
@ConditionalOnMissingBean(ICacheService.class)
public class SnowdriftCaffeineConfiguration {

    private final SnowdriftCacheProperties properties;

    public SnowdriftCaffeineConfiguration(SnowdriftCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * 自定义 CacheManager，使用 CacheProperties 中的 TTL 配置
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterAccess(properties.getKeyTtl())
                .expireAfterWrite(properties.getKeyTtl());
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);
        return manager;
    }

    @Bean
    public ICacheService caffeineCacheService(CacheSerializer serializer) {
        return new SnowdriftCaffeineCacheServiceImpl(properties, serializer);
    }
}
