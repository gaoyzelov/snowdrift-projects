package com.snowdrift.framework.cache.caffeine.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.caffeine.service.SnowdriftCaffeineCacheServiceImpl;
import com.snowdrift.framework.cache.config.SnowdriftCacheConfiguration;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
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
 * <p>
 * {@code before = SnowdriftCacheConfiguration.class} 不可省略：{@link SnowdriftCacheConfiguration}
 * 中的切面以 {@code @ConditionalOnBean(ICacheService.class)} 判断后端是否就绪，而该条件在解析顺序上
 * 只能看到先处理过的配置类注册的 Bean。{@code cache.caffeine.*} 恰好排在 {@code cache.config.*} 之前，
 * 当前顺序本就正确，但显式声明可避免今后改包名/改名时切面静默消失（Redis 侧正是踩了这个坑）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(afterName = {
        "com.snowdrift.framework.cache.redis.config.SnowdriftRedisConfiguration"
}, before = SnowdriftCacheConfiguration.class)
public class SnowdriftCaffeineConfiguration {

    private final SnowdriftCacheProperties properties;

    public SnowdriftCaffeineConfiguration(SnowdriftCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * CacheManager（Caffeine，使用 {@link SnowdriftCacheProperties} 中的 TTL 配置）
     * <p>
     * <b>这里的 {@code @ConditionalOnMissingBean} 是"兜底定位"，不是与 Redis 侧的不一致</b>：
     * 本类整体是最低优先级的后备实现（见类注解中的 {@code afterName} 与
     * {@code @ConditionalOnMissingBean(ICacheService)}），其 {@code CacheManager} 也应让位于
     * Redis 侧与使用者自定义的实现。
     * </p>
     * <p>
     * 对应地，{@code SnowdriftRedisConfiguration#cacheManager} <b>故意不加</b>该条件，
     * 原因见那里的说明——请勿把它当成遗漏去"补齐"，那会导致序列化器与 TTL 配置静默失效。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                .expireAfterWrite(properties.getKeyTtl());
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);
        return manager;
    }

    /**
     * Caffeine 实现：仅当容器中没有其它 {@code ICacheService}（Redis/Redisson）时注册。
     * <p>条件放在方法上而不是类上：类级条件会连带关掉 {@code CacheManager}，
     * 使业务自定义 {@code ICacheService} 时 {@code @Cacheable} 静默退化为 Boot 默认缓存。</p>
     */
    @Bean
    @ConditionalOnMissingBean(ICacheService.class)
    public ICacheService caffeineCacheService(ICacheSerializer serializer) {
        return new SnowdriftCaffeineCacheServiceImpl(properties, serializer);
    }
}
