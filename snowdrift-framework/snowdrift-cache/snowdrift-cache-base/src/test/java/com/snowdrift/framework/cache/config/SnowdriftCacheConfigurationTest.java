package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.SerializerType;
import com.snowdrift.framework.cache.aspect.DistributedLockAspect;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import com.snowdrift.framework.cache.handler.SnowdriftKeyGenerator;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.FastJson2CacheSerializer;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftCacheConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 缓存核心自动配置单元测试
 * @since 1.0.0
 */
class SnowdriftCacheConfigurationTest {

    /** @EnableCaching 需要一个 CacheManager 才能启动；真实应用由各后端子模块提供 */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class))
            .withBean("testCacheManager", org.springframework.cache.CacheManager.class,
                    org.springframework.cache.concurrent.ConcurrentMapCacheManager::new);

    @Test
    @DisplayName("默认注册 Jackson 序列化器，并接入 CachingConfigurer 的降级处理器与 key 生成器")
    void registersDefaults() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ICacheSerializer.class);
            assertThat(context.getBean(ICacheSerializer.class)).isInstanceOf(JacksonCacheSerializer.class);

            SnowdriftCacheConfiguration configuration = context.getBean(SnowdriftCacheConfiguration.class);
            assertThat(configuration.errorHandler()).isInstanceOf(SnowdriftCachingErrorHandler.class);
            assertThat(configuration.keyGenerator()).isInstanceOf(SnowdriftKeyGenerator.class);
            assertThat(configuration.cacheManager()).isNull();
        });
    }

    @Test
    @DisplayName("snowdrift.cache.serializer=FASTJSON2 时切换为 Fastjson2 序列化器")
    void serializerSwitchByProperty() {
        runner.withPropertyValues("snowdrift.cache.serializer=FASTJSON2").run(context -> {
            assertThat(context.getBean(ICacheSerializer.class)).isInstanceOf(FastJson2CacheSerializer.class);
            assertThat(context.getBean(SnowdriftCacheProperties.class).getSerializer())
                    .isEqualTo(SerializerType.FASTJSON2);
        });
    }

    @Test
    @DisplayName("业务自定义 ICacheSerializer 生效（@ConditionalOnMissingBean）")
    void customSerializerWins() {
        ICacheSerializer custom = mock(ICacheSerializer.class);

        runner.withBean(ICacheSerializer.class, () -> custom).run(context -> {
            assertThat(context).hasSingleBean(ICacheSerializer.class);
            assertThat(context.getBean(ICacheSerializer.class)).isSameAs(custom);
        });
    }

    @Test
    @DisplayName("配置属性绑定：key-prefix / key-ttl / max-size")
    void bindsProperties() {
        runner.withPropertyValues(
                "snowdrift.cache.key-prefix=app",
                "snowdrift.cache.key-ttl=30s",
                "snowdrift.cache.max-size=256").run(context -> {
            SnowdriftCacheProperties properties = context.getBean(SnowdriftCacheProperties.class);

            assertThat(properties.getKeyPrefix()).isEqualTo("app");
            assertThat(properties.getKeyTtl()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.getMaxSize()).isEqualTo(256);
        });
    }

    @Test
    @DisplayName("默认配置值：空前缀、1 小时 TTL、Jackson、10000 条")
    void defaultPropertyValues() {
        runner.run(context -> {
            SnowdriftCacheProperties properties = context.getBean(SnowdriftCacheProperties.class);

            assertThat(properties.getKeyPrefix()).isEmpty();
            assertThat(properties.getKeyTtl()).isEqualTo(Duration.ofHours(1));
            assertThat(properties.getSerializer()).isEqualTo(SerializerType.JACKSON);
            assertThat(properties.getMaxSize()).isEqualTo(10000L);
        });
    }

    @Test
    @DisplayName("切面按 Bean 条件装配：无 ICacheService / IDistributedLockService 时不注册")
    void aspectsAreConditional() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(RepeatSubmitAspect.class);
            assertThat(context).doesNotHaveBean(DistributedLockAspect.class);
        });

        runner.withBean(ICacheService.class, () -> mock(ICacheService.class)).run(context -> {
            assertThat(context).hasSingleBean(RepeatSubmitAspect.class);
            assertThat(context).doesNotHaveBean(DistributedLockAspect.class);
        });

        runner.withBean(IDistributedLockService.class, () -> mock(IDistributedLockService.class)).run(context -> {
            assertThat(context).hasSingleBean(DistributedLockAspect.class);
            assertThat(context).doesNotHaveBean(RepeatSubmitAspect.class);
        });
    }

    @Test
    @DisplayName("非法配置值（TTL 非 Duration 格式）导致启动失败，属快速失败")
    void invalidPropertyFailsFast() {
        runner.withPropertyValues("snowdrift.cache.key-ttl=not-a-duration").run(context ->
                assertThat(context).hasFailed());
    }
}
