package com.snowdrift.framework.cache.caffeine;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import com.snowdrift.framework.cache.caffeine.config.SnowdriftCaffeineConfiguration;
import com.snowdrift.framework.cache.caffeine.service.SnowdriftCaffeineCacheServiceImpl;
import com.snowdrift.framework.cache.config.SnowdriftCacheConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftCaffeineConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Caffeine 自动配置单元测试（兜底优先级）
 * @since 1.0.0
 */
class SnowdriftCaffeineConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class,
                    SnowdriftCaffeineConfiguration.class));

    @Test
    @DisplayName("无其它 ICacheService 时装配 Caffeine 实现与 CacheManager，并激活防重切面")
    void registersCaffeineBackend() {
        runner.withPropertyValues("snowdrift.cache.key-prefix=app", "snowdrift.cache.max-size=128").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ICacheService.class);
            assertThat(context.getBean(ICacheService.class)).isInstanceOf(SnowdriftCaffeineCacheServiceImpl.class);

            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);

            assertThat(context).hasSingleBean(RepeatSubmitAspect.class);
        });
    }

    @Test
    @DisplayName("已存在其它 ICacheService 时不注册 Caffeine 实现，但 CacheManager 仍装配（回归）")
    void backsOffWhenAnotherCacheServiceExists() {
        ICacheService redisLike = mock(ICacheService.class);

        runner.withBean(ICacheService.class, () -> redisLike).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ICacheService.class);
            assertThat(context.getBean(ICacheService.class)).isSameAs(redisLike);
            assertThat(context).doesNotHaveBean(SnowdriftCaffeineCacheServiceImpl.class);

            // 回归：自定义 ICacheService 不应连带取消 CacheManager（否则 @Cacheable 会静默退化或被 Boot 默认实现接管）
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
        });
    }

    @Test
    @DisplayName("业务自定义 CacheManager 时 Caffeine 的 CacheManager 退让")
    void customCacheManagerWins() {
        runner.withBean("customCacheManager", CacheManager.class, ConcurrentMapCacheManager::new).run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(ConcurrentMapCacheManager.class);
        });
    }

    @Test
    @DisplayName("Caffeine 实现全局按 keyTtl 过期，且 per-key TTL 可用（不再抛 UnsupportedOperationException）")
    void perKeyTtlSupported() {
        runner.withPropertyValues("snowdrift.cache.key-ttl=1h").run(context -> {
            ICacheService cacheService = context.getBean(ICacheService.class);

            cacheService.put("k", "v", java.time.Duration.ofMinutes(5));
            cacheService.putIfAbsent("k2", "v", java.time.Duration.ofMinutes(5));

            assertThat(cacheService.getExpire("k")).isBetween(1L, 300L);
            assertThat(cacheService.getExpire("k2")).isBetween(1L, 300L);
        });
    }
}
