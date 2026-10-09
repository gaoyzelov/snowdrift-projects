package com.snowdrift.framework.cache.caffeine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy.VarExpiration;
import com.snowdrift.framework.cache.AbstractCacheService;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

/**
 * Caffeine 本地缓存实现
 * <p>
 * 基于 Caffeine 的<b>可变过期策略</b>（{@code expireAfter(Expiry)} 配合 {@code Policy.VarExpiration}）
 * 实现 per-key TTL，因此带 TTL 的 {@code put} / {@code putIfAbsent} 以及 {@code expire} / {@code getExpire}
 * 均可用；未显式指定 TTL 时取 {@link SnowdriftCacheProperties#getKeyTtl()} 作为默认值。
 * </p>
 * <p>
 * 仍不支持（Caffeine 无对应结构与语义）：哈希操作 {@code hget} / {@code hput} / {@code hdelete}
 * 与原子自增 {@code increment}，调用将抛出 {@link UnsupportedOperationException}。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftCaffeineCacheServiceImpl extends AbstractCacheService {

    private final Cache<String, String> cache;

    /**
     * 可变过期策略视图，用于 per-key TTL
     */
    private final VarExpiration<String, String> varExpiration;

    /**
     * "实际永不过期"判定阈值（100 年）
     * <p>全局 key-ttl 非正时，条目按 {@link Long#MAX_VALUE} 纳秒（约 292 年）设置过期时间；
     * Caffeine 把"截止时间 - 当前时间"换算成剩余时长时会溢出，实测剩余约为 146 年。
     * 因此不能与哨兵值精确比较，凡剩余时长超过 100 年一律按"永不过期"处理。</p>
     */
    private static final Duration NEVER_EXPIRES_THRESHOLD = Duration.ofDays(36500);

    public SnowdriftCaffeineCacheServiceImpl(SnowdriftCacheProperties properties, ICacheSerializer serializer) {
        super(properties, serializer);
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaxSize())
                // 必须用 expireAfter(Expiry)：一旦设置 expireAfterWrite/Access，可变过期策略即被关闭，
                // VarExpiration 不可用，per-key TTL 也无从实现（这正是此前 put(.., ttl) 抛异常的根因）。
                .expireAfter(new Expiry<String, String>() {
                    @Override
                    public long expireAfterCreate(String key, String value, long currentTime) {
                        return defaultTtlNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
                        return defaultTtlNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
                        // 读取不延长过期时间，与原 expireAfterWrite 的语义保持一致
                        return currentDuration;
                    }
                })
                .build();
        this.varExpiration = cache.policy().expireVariably()
                .orElseThrow(() -> new IllegalStateException(
                        "Caffeine 可变过期策略不可用：缓存必须以 expireAfter(Expiry) 构建"));
    }

    /**
     * 默认 TTL（纳秒）；配置为非正数时视为永不过期
     */
    private long defaultTtlNanos() {
        Duration ttl = properties.getKeyTtl();
        long nanos = ttl == null ? 0L : ttl.toNanos();
        return nanos > 0 ? nanos : Long.MAX_VALUE;
    }

    // =================== AbstractCacheService 抽象方法实现 ===================

    @Override
    public String doGet(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    protected String doHget(String key, String hashKey) {
        throw new UnsupportedOperationException("Caffeine 不支持 hget 操作");
    }

    @Override
    public void doPut(String key, String value) {
        cache.put(key, value);
    }

    @Override
    protected void doHput(String key, String hashKey, String value) {
        throw new UnsupportedOperationException("Caffeine 不支持 hput 操作");
    }

    @Override
    public void doPut(String key, String value, Duration ttl) {
        varExpiration.put(key, value, ttl);
    }

    @Override
    public boolean doPutIfAbsent(String key, String value) {
        Object existing = cache.asMap().putIfAbsent(key, value);
        return existing == null;
    }

    @Override
    public boolean doPutIfAbsent(String key, String value, Duration ttl) {
        // putIfAbsent 返回旧值：非 null 表示键已存在（未写入），null 表示本次写入成功
        return varExpiration.putIfAbsent(key, value, ttl) == null;
    }

    @Override
    public boolean doDelete(String key) {
        return cache.asMap().remove(key) != null;
    }

    @Override
    protected boolean doHdelete(String key, String hashKey) {
        throw new UnsupportedOperationException("Caffeine 不支持 hdelete 操作");
    }

    @Override
    public long doBatchDelete(List<String> keys) {
        long count = 0;
        for (String key : keys) {
            if (doDelete(key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean doExists(String key) {
        return cache.getIfPresent(key) != null;
    }

    /**
     * 动态修改过期时间
     * <p>与 Redis 实现对齐：键不存在时返回 {@code false}。存在性判断与设置无法原子完成，
     * 存在极小竞态窗口（键恰在此期间过期则仍返回 true），Redis 侧同样存在该竞态。</p>
     */
    @Override
    public boolean doExpire(String key, Duration ttl) {
        if (varExpiration.getExpiresAfter(key).isEmpty()) {
            return false;
        }
        varExpiration.setExpiresAfter(key, ttl);
        return true;
    }

    /**
     * 查询剩余过期时间
     * <p>与 Redis 实现对齐：单位为秒，键不存在返回 {@code -2}，<b>永不过期返回 {@code -1}</b>。
     * 全局 key-ttl 配置为非正数时条目按"永不过期"处理，此时直接返回秒数会得到 4.6e9 这类无意义数值，
     * 与接口契约（-1 表示永不过期）不符。</p>
     */
    @Override
    public long doGetExpire(String key) {
        return varExpiration.getExpiresAfter(key)
                .map(ttl -> ttl.compareTo(NEVER_EXPIRES_THRESHOLD) >= 0 ? -1L : ttl.toSeconds())
                .orElse(-2L);
    }

    /**
     * Caffeine 原生不支持原子自增，调用将抛出 {@link UnsupportedOperationException}
     */
    @Override
    protected long doIncrement(String key, Duration ttl) {
        throw new UnsupportedOperationException("Caffeine 不支持 increment 操作");
    }
}
