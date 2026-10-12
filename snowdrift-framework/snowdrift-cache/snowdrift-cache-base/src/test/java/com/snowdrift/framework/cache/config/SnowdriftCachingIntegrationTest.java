package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.base.util.EncryptUtil;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftCachingIntegrationTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description @Cacheable 端到端装配验证：框架 KeyGenerator 与 CacheErrorHandler 必须真正被缓存拦截器采用
 * @since 1.0.0
 */
class SnowdriftCachingIntegrationTest {

    /** 带 @Cacheable 的业务 Bean：不指定 key/keyGenerator，期望走框架的 CachingConfigurer 默认值 */
    static class UserService {

        final AtomicInteger invocations = new AtomicInteger();

        @Cacheable(cacheNames = "users")
        public String find(Long id) {
            invocations.incrementAndGet();
            return "user-" + id;
        }

        /** 通过方法读取而非直接读字段：容器里拿到的是 CGLIB 代理，代理实例的字段与目标对象各自独立 */
        public int invocationCount() {
            return invocations.get();
        }
    }

    /** 探测用 CacheManager：get 直接抛异常，用于验证降级处理器真的接在拦截器上 */
    static class ExplodingCacheManager extends ConcurrentMapCacheManager {

        @Override
        public Cache getCache(String name) {
            Cache delegate = super.getCache(name);
            return new Cache() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Object getNativeCache() {
                    return delegate.getNativeCache();
                }

                @Override
                public ValueWrapper get(Object key) {
                    throw new IllegalStateException("cache down");
                }

                @Override
                public <T> T get(Object key, Class<T> type) {
                    throw new IllegalStateException("cache down");
                }

                @Override
                public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
                    throw new IllegalStateException("cache down");
                }

                @Override
                public void put(Object key, Object value) {
                    throw new IllegalStateException("cache down");
                }

                @Override
                public void evict(Object key) {
                    throw new IllegalStateException("cache down");
                }

                @Override
                public void clear() {
                    throw new IllegalStateException("cache down");
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CacheableConfig {

        @Bean
        UserService userService() {
            return new UserService();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ExplodingConfig {

        @Bean
        UserService userService() {
            return new UserService();
        }

        @Bean
        CacheManager cacheManager() {
            return new ExplodingCacheManager();
        }
    }

    /** 宿主自定义 KeyGenerator：所有 key 统一为固定值，便于断言它是否真的被采用 */
    static class HostKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            return "host-key";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class HostKeyGeneratorConfig {

        @Bean
        UserService userService() {
            return new UserService();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        KeyGenerator hostKeyGenerator() {
            return new HostKeyGenerator();
        }
    }

    @Test
    @DisplayName("宿主自定义 KeyGenerator 时框架退避：@Cacheable 真正采用宿主的 key 生成器")
    void hostKeyGeneratorWins() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class))
                .withUserConfiguration(HostKeyGeneratorConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UserService service = context.getBean(UserService.class);

                    assertThat(service.find(1L)).isEqualTo("user-1");

                    Cache cache = context.getBean(CacheManager.class).getCache("users");
                    assertThat(cache).isNotNull();
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> nativeCache = (Map<Object, Object>) cache.getNativeCache();
                    assertThat(nativeCache).containsKey("host-key");
                });
    }

    @Test
    @DisplayName("宿主提供自己的 CachingConfigurer 时容器仍能启动（不会出现多个 CachingConfigurer 而启动失败）")
    void hostCachingConfigurerDoesNotConflict() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class))
                .withUserConfiguration(CacheableConfig.class)
                .withBean("hostCachingConfigurer", CachingConfigurer.class, () -> new CachingConfigurer() {
                })
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("@Cacheable 默认使用框架 KeyGenerator（简单类名#方法:参数段摘要），而不是 Spring 的 SimpleKey")
    void cacheableUsesFrameworkKeyGenerator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class))
                .withUserConfiguration(CacheableConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UserService service = context.getBean(UserService.class);

                    assertThat(service.find(1L)).isEqualTo("user-1");
                    assertThat(service.find(1L)).isEqualTo("user-1");
                    // 第二次命中缓存，方法只执行一次
                    assertThat(service.invocationCount()).isEqualTo(1);

                    Cache cache = context.getBean(CacheManager.class).getCache("users");
                    assertThat(cache).isNotNull();
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> nativeCache = (Map<Object, Object>) cache.getNativeCache();
                    assertThat(nativeCache).containsKey(
                            UserService.class.getSimpleName() + "#find:" + EncryptUtil.sha256("Long:1").substring(0, 32));
                });
    }

    @Test
    @DisplayName("@Cacheable 的缓存故障经框架 CacheErrorHandler 降级，业务方法照常执行")
    void cacheFailureDegradesToMethodInvocation() {
        // 只断言“没抛异常”不足以证明是框架的降级处理器起了作用（Spring 默认的 SimpleCacheErrorHandler 会直接抛），
        // 因此同时抓取 SnowdriftCachingErrorHandler 的日志，确认确实是它接在拦截器上
        ch.qos.logback.classic.Logger handlerLogger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(SnowdriftCachingErrorHandler.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        handlerLogger.addAppender(appender);
        try {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(SnowdriftCacheConfiguration.class))
                    .withUserConfiguration(ExplodingConfig.class)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        UserService service = context.getBean(UserService.class);

                        // 缓存不可用时不能抛异常，直接穿透到方法
                        assertThat(service.find(2L)).isEqualTo("user-2");
                        assertThat(service.invocationCount()).isEqualTo(1);
                    });
        } finally {
            handlerLogger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list)
                .as("框架降级处理器必须被 @Cacheable 真正调用（默认 SimpleCacheErrorHandler 会直接抛异常）")
                .isNotEmpty();
        assertThat(appender.list.get(0).getFormattedMessage()).contains("[Cache] GET 失败");
    }
}
