package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Duration;

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

    protected final Duration timeout;
    protected final Duration idle;

    protected AbstractTokenStore(Duration timeout, Duration idle) {
        this.timeout = timeout;
        this.idle = idle;
    }

    // =================== 模板方法（final） ===================

    @Override
    public final void put(String token, SecurityContext context, Duration timeout) {
        Duration ttl = resolveTtl(timeout);
        long now = System.currentTimeMillis();
        // timeout <= 0 表示「永不过期」：绝对过期时间戳置为 Long.MAX_VALUE，
        // 避免 now + 非正 ttl 导致写入即过期（对齐 Sa-Token 的 -1 约定）。
        long expireAt = (ttl == null || ttl.toMillis() <= 0) ? Long.MAX_VALUE : now + ttl.toMillis();
        TokenEntry entry = new TokenEntry(context, expireAt, now);
        doPut(token, entry, ttl);
        log.trace("TokenStore 写入: token={}, ttl={}s", token,
                ttl == null || ttl.toMillis() <= 0 ? -1 : ttl.getSeconds());
    }

    @Override
    public final SecurityContext get(String token) {
        TokenEntry entry = doGet(token);
        if (entry == null) {
            return null;
        }

        long now = System.currentTimeMillis();

        // 1. 绝对过期检查（expireAt 为 Long.MAX_VALUE 表示永不过期）
        if (entry.expireAt != Long.MAX_VALUE && entry.expireAt < now) {
            remove(token);
            log.trace("TokenStore 绝对过期: token={}", token);
            return null;
        }

        // 2. 闲置过期检查（idle <= 0 表示不限制闲置时长，跳过闲置淘汰）
        if (idle.toMillis() > 0 && (now - entry.lastActiveAt) > idle.toMillis()) {
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
     * @param token Token 值
     * @param entry TokenEntry（含 context、expireAt、lastActiveAt）
     * @param ttl   过期时间；{@code null} 或 {@code <= 0} 表示永不过期，子类此时不应设置介质 TTL
     */
    protected abstract void doPut(String token, TokenEntry entry, Duration ttl);

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

    protected Duration resolveTtl(Duration ttl) {
        // 显式传入非正 TTL 时回退到模块配置的 timeout；
        // 若配置同样非正（<=0），下游按「永不过期」处理（ttl.toMillis() <= 0）。
        return ttl == null || ttl.isZero() || ttl.isNegative() ? timeout : ttl;
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
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenEntry implements Serializable {
        private SecurityContext context;
        private long expireAt;
        private long lastActiveAt;
    }
}
