package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;

/**
 * Token 存储抽象接口
 * <p>
 * 屏蔽底层存储差异（内存 / Redis），提供统一的 Token ↔ SecurityContext 映射存取能力。
 * </p>
 * <p>
 * <b>当前实现：</b>仅提供 {@link InMemoryTokenStore}（基于 ConcurrentHashMap），适用于单节点开发调试。
 * <br>
 * <b>待实现：</b>RedisTokenStore，分布式环境下通过 {@code StringRedisTemplate} 存取 Token。
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
     * @param timeout 过期时间（秒），0 或负数表示永不过期
     */
    void put(String token, SecurityContext context, long timeout);

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
