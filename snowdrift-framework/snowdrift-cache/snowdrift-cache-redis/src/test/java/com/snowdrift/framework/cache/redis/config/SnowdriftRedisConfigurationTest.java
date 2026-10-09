package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.SnowdriftCacheConfiguration;
import com.snowdrift.framework.cache.redis.service.SnowdriftRedisCacheServiceImpl;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftRedisConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description Redis 缓存自动配置单元测试（mock RedisConnectionFactory，不连真实 Redis）
 * @since 1.0.0
 */
class SnowdriftRedisConfigurationTest {

    private final RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class,
                    SnowdriftRedisConfiguration.class))
            .withBean("redisConnectionFactory", RedisConnectionFactory.class, () -> connectionFactory);

    @Test
    @DisplayName("存在 RedisConnectionFactory 时装配 Redis CacheManager 与 ICacheService")
    void registersRedisBackend() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);

            assertThat(context).hasSingleBean(ICacheService.class);
            assertThat(context.getBean(ICacheService.class)).isInstanceOf(SnowdriftRedisCacheServiceImpl.class);
        });
    }

    @Test
    @DisplayName("回归：Redis 后端下 @RepeatSubmit 切面必须装配（自动配置排序，曾是静默失效）")
    void repeatSubmitAspectRegisteredWithRedisBackend() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            // SnowdriftCacheConfiguration 的切面用 @ConditionalOnBean(ICacheService.class) 判断后端就绪，
            // 该条件只认“先处理过”的配置类；不把 Redis 配置排到核心配置之前，切面会静默不注册
            assertThat(context).hasSingleBean(com.snowdrift.framework.cache.aspect.RepeatSubmitAspect.class);
        });
    }

    @Test
    @DisplayName("回归：Redisson 可用时 @DistributedLock 切面必须装配（同样受自动配置排序影响）")
    void distributedLockAspectRegisteredWithRedisson() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class,
                        SnowdriftRedisConfiguration.class, SnowdriftRedissonConfiguration.class))
                .withBean("redisConnectionFactory", RedisConnectionFactory.class, () -> connectionFactory)
                // 用 mock 顶掉真实 RedissonClient：本用例只验证 Bean 装配，不连真实 Redis
                .withBean(org.redisson.api.RedissonClient.class, () -> mock(org.redisson.api.RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(com.snowdrift.framework.cache.IDistributedLockService.class);
                    assertThat(context).hasSingleBean(com.snowdrift.framework.cache.aspect.DistributedLockAspect.class);
                });
    }

    @Test
    @DisplayName("回归：Redis 与 Caffeine 同时存在时 Redis 优先，Caffeine 兜底退让（装配优先级首次被验证）")
    void redisWinsOverCaffeine() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class,
                        com.snowdrift.framework.cache.caffeine.config.SnowdriftCaffeineConfiguration.class,
                        SnowdriftRedisConfiguration.class))
                .withBean("redisConnectionFactory", RedisConnectionFactory.class, () -> connectionFactory)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(ICacheService.class)
                            .isInstanceOf(SnowdriftRedisCacheServiceImpl.class);
                    assertThat(context).hasSingleBean(CacheManager.class);
                    assertThat(context).getBean(CacheManager.class).isInstanceOf(RedisCacheManager.class);
                    // Caffeine 侧整体退让后，切面仍必须装配（核心配置排在最后）
                    assertThat(context).hasSingleBean(com.snowdrift.framework.cache.aspect.RepeatSubmitAspect.class);
                });
    }

    @Test
    @DisplayName("框架专用 String-String RedisTemplate 按名字注册，不再 @Primary 劫持按类型注入")
    void registersStringRedisTemplateByName() {
        // 引入 Boot 的 RedisAutoConfiguration，构造与真实应用一致的多 RedisTemplate 环境
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
                        SnowdriftCacheConfiguration.class, SnowdriftRedisConfiguration.class))
                .withBean("redisConnectionFactory", RedisConnectionFactory.class, () -> connectionFactory)
                .run(context -> {
            assertThat(context).hasBean("snowdriftRedisTemplate");

            RedisTemplate<?, ?> template = context.getBean("snowdriftRedisTemplate", RedisTemplate.class);
            assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
            assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);

            // 特意不加 @Primary：应用按类型注入 RedisTemplate 时会因多个候选而**显式报错**，
            // 而不是静默拿到 String-String 模板后在运行期出错；框架内部按名字/限定符注入
            assertThatThrownBy(() -> context.getBean(RedisTemplate.class))
                    .isInstanceOf(org.springframework.beans.factory.NoUniqueBeanDefinitionException.class);
            assertThat(context.getBean("snowdriftRedisTemplate", RedisTemplate.class)).isSameAs(template);
        });
    }

    @Test
    @DisplayName("key 前缀与 TTL 进入 CacheManager 配置（prefix:cacheName:key）")
    void prefixAndTtlApplied() {
        runner.withPropertyValues("snowdrift.cache.key-prefix=app", "snowdrift.cache.key-ttl=30s").run(context -> {
            RedisCacheConfiguration configuration = cacheConfiguration(context, "users");

            assertThat(configuration.getKeyPrefixFor("users")).isEqualTo("app:users:");
            assertThat(configuration.getTtl()).isEqualTo(java.time.Duration.ofSeconds(30));
        });
    }

    @Test
    @DisplayName("未配置前缀时 key 形如 cacheName:key")
    void defaultPrefix() {
        runner.run(context ->
                assertThat(cacheConfiguration(context, "users").getKeyPrefixFor("users")).isEqualTo("users:"));
    }

    @Test
    @DisplayName("默认（Jackson）值序列化器写入 @class 类型信息（Spring Cache 拿不到返回类型，必须落盘）")
    void jacksonValueSerializerWritesTypeInfo() {
        runner.run(context -> {
            // 用非标量负载：标量元素不带类型标记，无法体现多态还原能力
            byte[] bytes = write(context, "users", java.util.Map.of("k", "v"));

            assertThat(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).contains("@class");
        });
    }

    @Test
    @DisplayName("serializer=FASTJSON2 不影响 @Cacheable 的值序列化器：仍是带 @class 的那一套（回归）")
    void serializerSwitchDoesNotAffectCacheable() {
        runner.withPropertyValues("snowdrift.cache.serializer=FASTJSON2").run(context -> {
            byte[] bytes = write(context, "users", java.util.Map.of("k", "v"));
            String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            // 刻意不随配置切换：Jackson 的 @class 与 fastjson2 的 @type 互不可读，
            // 若随配置切换，改一次配置就会让既有 @Cacheable 缓存整体读不出来
            assertThat(json).contains("@class").doesNotContain("@type");

            // 该配置只影响 ICacheService 的序列化器（两个实现已对齐，值格式一致）
            assertThat(context.getBean(com.snowdrift.framework.cache.serialize.ICacheSerializer.class))
                    .isInstanceOf(com.snowdrift.framework.cache.serialize.FastJson2CacheSerializer.class);
        });
    }

    /** 取指定 cache 的 Redis 配置：CacheManager 开了事务感知，取到的是装饰器，需要取回目标 cache */
    private static RedisCacheConfiguration cacheConfiguration(
            org.springframework.boot.test.context.assertj.AssertableApplicationContext context, String cacheName) {
        org.springframework.cache.Cache cache = context.getBean(CacheManager.class).getCache(cacheName);
        assertThat(cache).isNotNull();
        if (cache instanceof TransactionAwareCacheDecorator decorator) {
            cache = decorator.getTargetCache();
        }
        return ((RedisCache) cache).getCacheConfiguration();
    }

    private static byte[] write(org.springframework.boot.test.context.assertj.AssertableApplicationContext context,
                                String cacheName, Object value) {
        java.nio.ByteBuffer buffer = cacheConfiguration(context, cacheName).getValueSerializationPair().write(value);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @Test
    @DisplayName("业务自定义 ICacheService 时框架实现退让，但 CacheManager/RedisTemplate 仍装配（回归）")
    void customCacheServiceWins() {
        ICacheService custom = mock(ICacheService.class);

        runner.withBean(ICacheService.class, () -> custom).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ICacheService.class);
            assertThat(context.getBean(ICacheService.class)).isSameAs(custom);
            assertThat(context).doesNotHaveBean(SnowdriftRedisCacheServiceImpl.class);

            // 回归：自定义 ICacheService 不得连带关掉 CacheManager / RedisTemplate（曾因类级条件全部消失）
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);
            assertThat(context).hasBean("snowdriftRedisTemplate");
        });
    }
}
