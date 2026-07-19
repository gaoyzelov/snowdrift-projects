package com.snowdrift.framework.cache.redisson.config;

import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.cache.redisson.service.RedissonCacheServiceImpl;
import com.snowdrift.framework.cache.redisson.service.RedissonLockService;
import com.snowdrift.framework.cache.serialize.CacheSerializer;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Redisson 缓存与分布式锁自动配置
 * <p>
 * 直接从 {@link RedisProperties}（兼容 {@code spring.data.redis.*}）构建 {@link RedissonClient}，
 * 支持单节点、哨兵和集群模式。使用 {@link StringCodec} 存储 JSON 字符串，
 * 序列化由 {@link CacheSerializer} 统一处理。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(beforeName = "com.snowdrift.framework.cache.redis.config.SnowdriftRedisConfiguration")
@ConditionalOnClass(RedissonClient.class)
public class SnowdriftRedissonConfiguration {

    /**
     * 看门狗锁续期时间（毫秒）。
     * <p>Redisson 默认 30s，缩短至 15s 可更快释放失联客户端持有的锁。</p>
     */
    private static final long LOCK_WATCHDOG_TIMEOUT_MS = 15_000L;

    private RedissonClient redissonClient;

    private final CacheProperties cacheProperties;

    public SnowdriftRedissonConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }


    @PreDestroy
    public void destroy() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }

    /**
     * 初始化 RedissonClient 客户端
     * <p>
     * 根据配置自动选择集群、哨兵或单节点模式。
     * 使用 {@link StringCodec} 以 JSON 字符串存储，与 Caffeine / Redis 后端数据格式一致。
     * </p>
     *
     * @param redisProperties Spring Boot Redis 配置属性
     * @return {@link RedissonClient}
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        // 使用 StringCodec，序列化由 CacheSerializer 在服务层统一处理
        config.setCodec(StringCodec.INSTANCE);
        String password = redisProperties.getPassword();

        // 转发 spring.data.redis.timeout 到 Redisson（Redisson 以毫秒为单位）
        int timeoutMs = redisProperties.getTimeout() != null ? (int) redisProperties.getTimeout().toMillis() : 3000;

        String uriPrefix = redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled() ? "rediss://" : "redis://";

        if (redisProperties.getCluster() != null) {
            configureCluster(config, redisProperties.getCluster().getNodes(), uriPrefix, timeoutMs);
        } else if (redisProperties.getSentinel() != null) {
            configureSentinel(config, redisProperties.getSentinel(), redisProperties.getDatabase(), uriPrefix, timeoutMs);
        } else {
            configureSingle(config, redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase(), uriPrefix, timeoutMs);
            // 转发 spring.data.redis 连接池配置（兼容 Lettuce 和 Jedis）
            applyPoolConfig(config, redisProperties);
        }

        // 密码统一在 Config 层设置（Redisson 4.x 已废弃各 ServerConfig 的 setPassword）
        if (StringUtils.isNotBlank(password)) {
            config.setPassword(password);
        }

        config.setLockWatchdogTimeout(LOCK_WATCHDOG_TIMEOUT_MS);
        this.redissonClient = Redisson.create(config);
        return this.redissonClient;
    }

    /**
     * 转发 spring.data.redis 连接池配置到 Redisson（仅单节点模式适用）
     * <p>兼容 Lettuce 和 Jedis 两种客户端配置。</p>
     */
    private void applyPoolConfig(Config config, RedisProperties redisProperties) {
        // 优先取 Lettuce pool，其次 Jedis pool
        RedisProperties.Pool pool = null;
        if (redisProperties.getLettuce() != null && redisProperties.getLettuce().getPool() != null) {
            pool =  redisProperties.getLettuce().getPool();
        } else if (redisProperties.getJedis() != null && redisProperties.getJedis().getPool() != null) {
            pool = redisProperties.getJedis().getPool();
        }
        if (pool == null) {
            return;
        }
        config.useSingleServer().setConnectionPoolSize(pool.getMaxActive());
        config.useSingleServer().setConnectionMinimumIdleSize(pool.getMinIdle());
    }

    /**
     * 集群模式配置
     */
    private void configureCluster(Config config, List<String> nodes, String uriPrefix, int timeoutMs) {
        String[] addresses = nodes.stream()
                .map(node -> uriPrefix + node)
                .toArray(String[]::new);
        config.useClusterServers()
                .addNodeAddress(addresses)
                .setTimeout(timeoutMs);
    }

    /**
     * 哨兵模式配置
     */
    private void configureSentinel(Config config, RedisProperties.Sentinel sentinel, int database, String uriPrefix, int timeoutMs) {
        String[] addresses = sentinel.getNodes().stream()
                .map(node -> uriPrefix + node)
                .toArray(String[]::new);
        config.useSentinelServers()
                .setMasterName(sentinel.getMaster())
                .addSentinelAddress(addresses)
                .setDatabase(database)
                .setTimeout(timeoutMs);
    }

    /**
     * 单节点模式配置
     */
    private void configureSingle(Config config, String host, int port, int database, String uriPrefix, int timeoutMs) {
        config.useSingleServer()
                .setAddress(uriPrefix + host + ":" + port)
                .setDatabase(database)
                .setTimeout(timeoutMs);
    }

    @Bean
    @ConditionalOnMissingBean(ICacheService.class)
    public ICacheService redissonCacheService(CacheSerializer serializer, RedissonClient redissonClient) {
        return new RedissonCacheServiceImpl(cacheProperties, serializer, redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLockService.class)
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new RedissonLockService(redissonClient);
    }

}
