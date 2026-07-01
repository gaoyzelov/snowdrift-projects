package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.aspect.DistributedLockAspect;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
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
public class SnowdriftCacheConfiguration {

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
