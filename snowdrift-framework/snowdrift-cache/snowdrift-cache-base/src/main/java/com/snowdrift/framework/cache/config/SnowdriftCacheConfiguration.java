package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.aspect.DistributedLockAspect;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import com.snowdrift.framework.cache.handler.SnowdriftKeyGenerator;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;

/**
 * 缓存核心自动配置
 * <p>
 * 启用 {@link SnowdriftCacheProperties} 配置绑定，注册 AOP 切面和 {@link ICacheSerializer} Bean。
 * 具体的 {@link ICacheService} 实现由各后端子模块提供：Redis（Lettuce/Jedis）存在时优先，
 * Caffeine 仅在容器中没有其它 {@code ICacheService} 时兜底注册；
 * Redisson 只提供 {@link IDistributedLockService}，不提供 {@code ICacheService}。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@EnableCaching
@AutoConfiguration
@EnableConfigurationProperties(SnowdriftCacheProperties.class)
public class SnowdriftCacheConfiguration {

    /**
     * 缓存序列化器 Bean
     * <p>
     * 统一使用基于 Jackson 的 {@link JacksonCacheSerializer}。
     * 消费者可注册自定义 {@link ICacheSerializer} Bean 完全替换。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ICacheSerializer.class)
    public ICacheSerializer cacheSerializer() {
        return new JacksonCacheSerializer();
    }

    /**
     * 缓存 Key 生成器与异常降级处理器的接入点
     *
     * <p>必须走 {@link CachingConfigurer}：Spring 的 {@code CacheAspectSupport} 只会从
     * {@code CachingConfigurer} 取 {@code CacheErrorHandler}（否则一律用 {@code SimpleCacheErrorHandler}，
     * 即缓存故障直接抛给业务），也不按类型从容器解析 {@code KeyGenerator}。
     * 因此把这两个组件注册成普通 Bean 是无效的。</p>
     *
     * <p><b>但也不能让本配置类直接 {@code implements CachingConfigurer}：</b>
     * Spring 只允许容器中存在一个 {@code CachingConfigurer}
     * （{@code AbstractCachingConfiguration#setConfigurers} 多于一个即抛
     * {@code IllegalStateException("N implementations of CachingConfigurer were found when only 1 was expected")}），
     * 宿主一旦自己也实现 {@code CachingConfigurer} 就会直接启动失败。</p>
     *
     * <p>因此这里注册为<b>带条件退避的独立 Bean</b>，且每个方法都优先采用宿主提供的同类 Bean：
     * <ul>
     *   <li>宿主自定义 {@code CachingConfigurer} → 本 Bean 退避，二者不再冲突</li>
     *   <li>宿主只自定义 {@code KeyGenerator}/{@code CacheErrorHandler} Bean → 由下方委托生效，不被静默忽略</li>
     *   <li>宿主不自定义 → 使用框架默认实现</li>
     * </ul>
     *
     * @param hostKeyGenerators   宿主提供的 KeyGenerator（可空）
     * @param hostErrorHandlers   宿主提供的 CacheErrorHandler（可空）
     * @return 缓存配置器；宿主已提供 CachingConfigurer 时退避
     */
    @Bean
    @ConditionalOnMissingBean(CachingConfigurer.class)
    public CachingConfigurer snowdriftCachingConfigurer(ObjectProvider<KeyGenerator> hostKeyGenerators,
                                                       ObjectProvider<CacheErrorHandler> hostErrorHandlers) {
        return new CachingConfigurer() {

            /**
             * 统一缓存 Key 生成器：简单类名#方法名[:参数段摘要]，参数段整体取 SHA-256 前 128 位摘要
             */
            @Override
            public KeyGenerator keyGenerator() {
                KeyGenerator host = hostKeyGenerators.getIfAvailable();
                return host != null ? host : new SnowdriftKeyGenerator();
            }

            /**
             * 缓存异常降级处理器，缓存故障时不阻断主流程
             */
            @Override
            public CacheErrorHandler errorHandler() {
                CacheErrorHandler host = hostErrorHandlers.getIfAvailable();
                return host != null ? host : new SnowdriftCachingErrorHandler();
            }
        };
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
