package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.aspect.DistributedLockAspect;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import com.snowdrift.framework.cache.handler.SnowdriftKeyGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;

/**
 * 缓存核心自动配置
 * <p>
 * 启用 {@link CacheProperties} 配置绑定，注册 AOP 切面。
 * 具体的 {@link ICacheService} 实现由各后端子模块提供，
 * 按类路径自动检测：Redisson → Redis（Lettuce/Jedis）→ Caffeine。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class SnowdriftCacheConfiguration implements CachingConfigurer {

    private final CacheProperties cacheProperties;

    public SnowdriftCacheConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 自定义 CacheManager 由各后端子模块提供，此处不覆盖。
     */

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
        return new SnowdriftKeyGenerator(cacheProperties.getKeyPrefix());
    }

    /**
     * 分布式锁 AOP 切面，仅在容器中存在 {@link DistributedLockService} 时激活
     */
    @Bean
    @ConditionalOnBean(DistributedLockService.class)
    public DistributedLockAspect distributedLockAspect(DistributedLockService lockService) {
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
