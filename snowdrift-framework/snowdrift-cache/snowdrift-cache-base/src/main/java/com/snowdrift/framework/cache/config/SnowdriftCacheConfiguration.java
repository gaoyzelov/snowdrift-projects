package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.aspect.DistributedLockAspect;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import com.snowdrift.framework.cache.enums.SerializerType;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import com.snowdrift.framework.cache.handler.SnowdriftKeyGenerator;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import com.snowdrift.framework.cache.serialize.FastJson2CacheSerializer;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;

/**
 * 缓存核心自动配置
 * <p>
 * 启用 {@link SnowdriftCacheProperties} 配置绑定，注册 AOP 切面和 {@link CacheSerializer} Bean。
 * 具体的 {@link ICacheService} 实现由各后端子模块提供，
 * 按类路径自动检测：Redisson → Redis（Lettuce/Jedis）→ Caffeine。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
@EnableCaching
@AutoConfiguration
@EnableConfigurationProperties(SnowdriftCacheProperties.class)
public class SnowdriftCacheConfiguration implements CachingConfigurer {

    private final SnowdriftCacheProperties properties;

    public SnowdriftCacheConfiguration(SnowdriftCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * 缓存序列化器 Bean
     * <p>
     * 根据 {@code snowdrift.cache.serializer} 配置选择实现：
     * {@code jackson}（默认）或 {@code fastjson2}。
     * 消费者可注册自定义 {@link CacheSerializer} Bean 完全替换。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(CacheSerializer.class)
    public CacheSerializer cacheSerializer() {
        SerializerType type = properties.getSerializer();
        if (type == SerializerType.FASTJSON2) {
            log.info("缓存序列化器: Fastjson2");
            return new FastJson2CacheSerializer();
        }
        log.info("缓存序列化器: Jackson");
        return new JacksonCacheSerializer();
    }

    /**
     * 缓存异常降级处理器，缓存故障时不阻断主流程
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SnowdriftCachingErrorHandler();
    }

    /**
     * 统一缓存 Key 生成器，格式：[prefix:]ClassName#methodName[:params...]
     */
    @Override
    public KeyGenerator keyGenerator() {
        return new SnowdriftKeyGenerator(properties.getKeyPrefix());
    }

    /**
     * 分布式锁 AOP 切面，仅在容器中存在 {@link IDistributedLockService} 时激活
     */
    @Bean
    @ConditionalOnBean(IDistributedLockService.class)
    public DistributedLockAspect distributedLockAspect(IDistributedLockService lockService) {
        return new DistributedLockAspect(lockService);
    }

    /**
     * 重复提交防护 AOP 切面
     */
    @Bean
    @ConditionalOnBean(ICacheService.class)
    public RepeatSubmitAspect repeatSubmitAspect(ICacheService cacheService) {
        return new RepeatSubmitAspect(cacheService);
    }
}
