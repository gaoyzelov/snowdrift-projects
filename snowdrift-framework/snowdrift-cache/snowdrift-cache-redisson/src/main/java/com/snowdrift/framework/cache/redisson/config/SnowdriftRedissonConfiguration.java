package com.snowdrift.framework.cache.redisson.config;

import com.snowdrift.framework.cache.CacheSerializer;
import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.cache.handler.SnowdriftCachingErrorHandler;
import com.snowdrift.framework.cache.handler.SnowdriftKeyGenerator;
import com.snowdrift.framework.cache.redisson.service.RedissonCacheServiceImpl;
import com.snowdrift.framework.cache.redisson.service.RedissonLockService;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Redisson 缓存与分布式锁自动配置
 * <p>
 * 直接从 {@link RedisProperties}（兼容 {@code spring.data.redis.*}）构建 {@link RedissonClient}，
 * 支持单节点、哨兵和集群模式。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(beforeName = "com.snowdrift.framework.cache.redis.config.SnowdriftRedisConfiguration")
@ConditionalOnClass(org.redisson.api.RedissonClient.class)
public class SnowdriftRedissonConfiguration implements CachingConfigurer {

    private static final String REDIS_URI_PREFIX = "redis://";

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
     * 按优先级依次检测集群、哨兵、单节点模式，密码为空时自动跳过设置。
     * </p>
     *
     * @param redisProperties Spring Boot Redis 配置属性
     * @return {@link RedissonClient}
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        // 使用 Jackson JSON 序列化，避免默认 MarshallingCodec 产生二进制乱码
        config.setCodec(new JsonJacksonCodec(CacheSerializer.getObjectMapper()));
        String password = redisProperties.getPassword();

        if (redisProperties.getCluster() != null) {
            configureCluster(config, redisProperties.getCluster().getNodes());
        } else if (redisProperties.getSentinel() != null) {
            configureSentinel(config, redisProperties.getSentinel(), redisProperties.getDatabase());
        } else {
            configureSingle(config, redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase());
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
     * 集群模式配置
     */
    private void configureCluster(Config config, List<String> nodes) {
        String[] addresses = nodes.stream()
                .map(node -> REDIS_URI_PREFIX + node)
                .toArray(String[]::new);
        config.useClusterServers().addNodeAddress(addresses);
    }

    /**
     * 哨兵模式配置
     */
    private void configureSentinel(Config config, RedisProperties.Sentinel sentinel, int database) {
        String[] addresses = sentinel.getNodes().stream()
                .map(node -> REDIS_URI_PREFIX + node)
                .toArray(String[]::new);
        config.useSentinelServers()
                .setMasterName(sentinel.getMaster())
                .addSentinelAddress(addresses)
                .setDatabase(database);
    }

    /**
     * 单节点模式配置
     */
    private void configureSingle(Config config, String host, int port, int database) {
        config.useSingleServer()
                .setAddress(REDIS_URI_PREFIX + host + ":" + port)
                .setDatabase(database);
    }

    @Bean
    @ConditionalOnMissingBean(ICacheService.class)
    public ICacheService redissonCacheService(CacheProperties cacheProperties,
                                               RedissonClient redissonClient) {
        return new RedissonCacheServiceImpl(cacheProperties, redissonClient);
    }

    @Bean
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new RedissonLockService(redissonClient);
    }

    /**
     * 缓存异常降级处理器，缓存故障时不阻断主流程
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new SnowdriftCachingErrorHandler();
    }

    /**
     * 统一缓存 Key 生成器，格式：[prefix:]ClassName#methodName[:params...]
     */
    @Override
    @Bean
    public KeyGenerator keyGenerator() {
        return new SnowdriftKeyGenerator(cacheProperties.getKeyPrefix());
    }

}
