package com.snowdrift.framework.cache.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 缓存异常降级处理器
 * <p>
 * 实现 {@link CacheErrorHandler}，拦截 Spring Cache 注解（{@code @Cacheable}、
 * {@code @CachePut}、{@code @CacheEvict}）执行过程中的异常，
 * 记录 warn 日志后静默降级，不向业务层抛出，保证缓存故障不影响主流程。
 * </p>
 *
 * <ul>
 *   <li>{@code get} 失败 → 降级为穿透到方法直接执行</li>
 *   <li>{@code put} 失败 → 跳过写入，方法正常返回</li>
 *   <li>{@code evict} 失败 → 跳过清除，方法结果不丢失</li>
 *   <li>{@code clear} 失败 → 跳过清空，记录日志</li>
 * </ul>
 *
 * @author gaoyzelov
 * @date 2026/6/13
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftCachingErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception,
                                    org.springframework.cache.Cache cache,
                                    Object key) {
        log.warn("[Cache] GET 失败，降级穿透执行: cache={}, key={}, error={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception,
                                    org.springframework.cache.Cache cache,
                                    Object key,
                                    Object value) {
        log.warn("[Cache] PUT 失败，跳过写入: cache={}, key={}, error={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception,
                                      org.springframework.cache.Cache cache,
                                      Object key) {
        log.warn("[Cache] EVICT 失败，跳过清除: cache={}, key={}, error={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception,
                                      org.springframework.cache.Cache cache) {
        log.warn("[Cache] CLEAR 失败，跳过清空: cache={}, error={}",
                cache.getName(), exception.getMessage());
    }
}
