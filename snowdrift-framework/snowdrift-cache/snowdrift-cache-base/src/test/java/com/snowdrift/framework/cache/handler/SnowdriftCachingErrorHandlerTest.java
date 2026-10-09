package com.snowdrift.framework.cache.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SnowdriftCachingErrorHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 缓存异常降级处理器单元测试：任何缓存异常都不得向业务抛出
 * @since 1.0.0
 */
class SnowdriftCachingErrorHandlerTest {

    private final SnowdriftCachingErrorHandler errorHandler = new SnowdriftCachingErrorHandler();
    private final Cache cache = mock(Cache.class);

    @Test
    @DisplayName("GET/PUT/EVICT/CLEAR 四类异常均静默降级")
    void swallowsAllCacheErrors() {
        when(cache.getName()).thenReturn("users");
        RuntimeException error = new IllegalStateException("redis 连接失败");

        assertThatCode(() -> {
            errorHandler.handleCacheGetError(error, cache, "key");
            errorHandler.handleCachePutError(error, cache, "key", "value");
            errorHandler.handleCacheEvictError(error, cache, "key");
            errorHandler.handleCacheClearError(error, cache);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("异常信息为 null 时也不抛异常（日志拼接安全）")
    void toleratesNullMessage() {
        when(cache.getName()).thenReturn("users");
        RuntimeException error = new RuntimeException();

        assertThatCode(() -> {
            errorHandler.handleCacheGetError(error, cache, null);
            errorHandler.handleCachePutError(error, cache, null, null);
            errorHandler.handleCacheEvictError(error, cache, null);
            errorHandler.handleCacheClearError(error, cache);
        }).doesNotThrowAnyException();
    }
}
