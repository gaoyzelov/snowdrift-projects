package com.snowdrift.framework.cache.caffeine.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.caffeine.service.CaffeineCacheServiceImpl;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import com.snowdrift.framework.cache.handler.SnowdriftKeyGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

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
public class SnowdriftCaffeineConfiguration implements CachingConfigurer {

    private final CacheProperties cacheProperties;

    public SnowdriftCaffeineConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 自定义 CacheManager，使用 CacheProperties 中的 TTL 配置
     */
    @Override
    @Bean
    public CacheManager cacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getMaxSize())
                .expireAfterWrite(cacheProperties.getKeyTtl().toMillis(), TimeUnit.MILLISECONDS);
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);
        return manager;
    }

    /**
     * 缓存异常降级处理器，缓存故障时不阻断主流程
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new SnowdriftCachingErrorHandler();
    }

    /**
     * 统一缓存 Key 生成器，格式：[prefix:]ClassName#methodName[:params...]
     */
    @Override
    @Bean
    public KeyGenerator keyGenerator() {
        return new SnowdriftKeyGenerator(cacheProperties.getKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean(ICacheService.class)
    public ICacheService caffeineCacheService() {
        return new CaffeineCacheServiceImpl(cacheProperties);
    }
}
