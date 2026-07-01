package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * TokenStore 抽象基类
 * <p>
 * 将 TTL 计算、绝对过期检查、闲置过期检查等公共语义上提到基类，
 * 子类仅负责存储介质的具体读写操作（内存 / Redis）。
 * </p>
 * <p>
 * 过期策略采用双维度独立管理：
 * </p>
 * <ul>
 *   <li><b>绝对过期</b>（{@link TokenEntry#expireAt}）：put 时确定，不可变</li>
 *   <li><b>闲置过期</b>（{@link TokenEntry#lastActiveAt}）：每次 get 时刷新</li>
 * </ul>
 *
 * @author gaoyzelov
 * @date 2026/6/14
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractTokenStore implements TokenStore {

    protected final long defaultTimeoutSeconds;
    protected final long idleTimeoutSeconds;

    protected AbstractTokenStore(long defaultTimeoutSeconds, long idleTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    // =================== 模板方法（final） ===================

    @Override
    public final void put(String token, SecurityContext context, long timeout) {
        long ttl = resolveTtl(timeout);
        long now = System.currentTimeMillis();
        long expireAt = now + ttl * 1000;
        TokenEntry entry = new TokenEntry(context, expireAt, now);
        doPut(token, entry, ttl);
        log.trace("TokenStore 写入: token={}, ttl={}s", token, ttl);
    }

    @Override
    public final SecurityContext get(String token) {
        TokenEntry entry = doGet(token);
        if (entry == null) {
            return null;
        }

        long now = System.currentTimeMillis();

        // 1. 绝对过期检查
        if (entry.expireAt < now) {
            remove(token);
            log.trace("TokenStore 绝对过期: token={}", token);
            return null;
        }

        // 2. 闲置过期检查
        if ((now - entry.lastActiveAt) > idleTimeoutSeconds * 1000) {
            remove(token);
            log.trace("TokenStore 闲置过期: token={}", token);
            return null;
        }

        // 3. 刷新活跃时间
        touch(token, entry);

        return entry.context;
    }

    // =================== 子类实现 ===================

    /**
     * 存储 TokenEntry
     *
     * @param token   Token 值
     * @param entry   TokenEntry（含 context、expireAt、lastActiveAt）
     * @param ttl     过期时间（秒），子类可按需用于设置 Redis TTL 等
     */
    protected abstract void doPut(String token, TokenEntry entry, long ttl);

    /**
     * 读取 TokenEntry
     *
     * @param token Token 值
     * @return TokenEntry，不存在返回 null
     */
    protected abstract TokenEntry doGet(String token);

    /**
     * 刷新活跃时间
     * <p>
     * 内存模式：更新 lastActiveAt 字段。
     * Redis 模式：EXPIRE 刷新 key TTL。
     * </p>
     *
     * @param token Token 值
     * @param entry 当前 TokenEntry
     */
    protected abstract void touch(String token, TokenEntry entry);

    // =================== 工具方法 ===================

    protected long resolveTtl(long timeout) {
        return timeout > 0 ? timeout : defaultTimeoutSeconds;
    }

    // =================== 共享数据模型 ===================

    /**
     * Token 存储条目
     * <p>
     * 双字段独立管理两种过期语义：
     * </p>
     * <ul>
     *   <li>{@code expireAt} — 绝对过期时间戳（毫秒），put 时确定，不可变</li>
     *   <li>{@code lastActiveAt} — 最后活跃时间戳（毫秒），每次 get 时刷新</li>
     * </ul>
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenEntry  implements Serializable {
        private SecurityContext context;
        private long expireAt;
        private long lastActiveAt;
    }
}
