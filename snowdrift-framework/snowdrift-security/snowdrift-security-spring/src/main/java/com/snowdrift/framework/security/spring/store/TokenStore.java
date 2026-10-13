package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;

import java.time.Duration;

/**
 * Token 存储抽象接口
 * <p>
 * 屏蔽底层存储差异（内存 / Redis），提供统一的 Token ↔ SecurityContext 映射存取能力。
 * </p>
 * <p>
 * <b>实现：</b>{@link InMemoryTokenStore}（基于 Guava Cache，LRU 上限 2048，适用于单节点/开发调试）
 * 与 {@link RedisTokenStore}（分布式环境，通过 {@code RedisTemplate} 存取 Token，多实例共享会话）。
 * Spring 实现默认要求 {@code RedisConnectionFactory} 并使用 Redis；内存实现必须通过配置显式启用。
 * <br>
 * <b>注意：</b>Sa-Token 模块（snowdrift-security-satoken）自带 Redis 集成插件，
 * 引入 {@code sa-token-dao-redis-jackson} 依赖即可启用，无需此接口。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
public interface TokenStore {

    /**
     * 存储 Token 与安全上下文的映射
     *
     * @param token   Token 值
     * @param context 安全上下文
     * @param timeout 过期时间；{@code null} 或 {@code <= 0} 表示永不过期，由 TokenStore 实现决定
     */
    void put(String token, SecurityContext context, Duration timeout);

    /**
     * 根据 Token 获取安全上下文
     *
     * @param token Token 值
     * @return 安全上下文，不存在或已过期时返回 null
     */
    SecurityContext get(String token);

    /**
     * 移除 Token 映射
     *
     * @param token Token 值
     */
    void remove(String token);
}
