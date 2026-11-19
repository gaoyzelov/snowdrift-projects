package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.redis.service.SnowdriftRedissonDistributedLockServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftRedissonConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/11/19
 * @description Redisson 自动配置单元测试（mock RedissonClient，不连真实 Redis）
 * @since 1.0.0
 */
class SnowdriftRedissonConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftRedissonConfiguration.class))
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class));

    @Test
    @DisplayName("默认装配定制器与分布式锁服务")
    void registersCustomizerAndLockService() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RedissonAutoConfigurationCustomizer.class);
            assertThat(context).hasSingleBean(IDistributedLockService.class);
            assertThat(context.getBean(IDistributedLockService.class))
                    .isInstanceOf(SnowdriftRedissonDistributedLockServiceImpl.class);
        });
    }

    @Test
    @DisplayName("定制器把 StringCodec 与 15s 看门狗写入 starter 的 Config（回归：此前从未生效）")
    void customizerAppliesCodecAndWatchdog() {
        runner.run(context -> {
            Config config = new Config();
            context.getBean(RedissonAutoConfigurationCustomizer.class).customize(config);

            assertThat(config.getCodec()).isSameAs(StringCodec.INSTANCE);
            assertThat(config.getLockWatchdogTimeout()).isEqualTo(15_000L);
        });
    }

    @Test
    @DisplayName("业务自定义 IDistributedLockService 时框架实现退让")
    void customLockServiceWins() {
        IDistributedLockService custom = mock(IDistributedLockService.class);

        runner.withBean(IDistributedLockService.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IDistributedLockService.class);
            assertThat(context.getBean(IDistributedLockService.class)).isSameAs(custom);
            assertThat(context).doesNotHaveBean(SnowdriftRedissonDistributedLockServiceImpl.class);
        });
    }

    @Test
    @DisplayName("snowdrift.cache.redisson.enabled=false 时定制器与锁服务一并退避")
    void disabledByProperty() {
        runner.withPropertyValues("snowdrift.cache.redisson.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RedissonAutoConfigurationCustomizer.class);
            assertThat(context).doesNotHaveBean(IDistributedLockService.class);
        });
    }
}
