package com.snowdrift.framework.cache.redisson.config;

import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.cache.redisson.service.RedissonCacheServiceImpl;
import com.snowdrift.framework.cache.redisson.service.RedissonLockService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;

/**
 * Redisson 缓存与分布式锁自动配置
 * <p>
 * 直接从 {@link RedisProperties}（兼容 {@code spring.data.redis.*}）构建 {@link RedissonClient}，
 * 支持单节点和集群模式。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnMissingBean(ICacheService.class)
@AutoConfigureBefore(name = "com.snowdrift.framework.cache.redis.config.SnowdriftRedisConfiguration")
public class SnowdriftRedissonConfiguration {

    private RedissonClient redissonClient;

    @PreDestroy
    public void destroy() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }

    /**
     * 初始化 RedissonClient 客户端
     * <p>
     * 支持单节点和集群模式，集群节点地址须使用 "redis://" 前缀。
     * </p>
     *
     * @return {@link RedissonClient}
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        if (redisProperties.getCluster() != null) {
            //集群模式配置
            List<String> nodes = redisProperties.getCluster().getNodes();

            List<String> clusterNodes = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                clusterNodes.add("redis://" + nodes.get(i));
            }
            ClusterServersConfig clusterServersConfig = config.useClusterServers()
                    .addNodeAddress(clusterNodes.toArray(new String[clusterNodes.size()]));

            if (StringUtils.isNotBlank(redisProperties.getPassword())) {
                clusterServersConfig.setPassword(redisProperties.getPassword());
            }
        } else {
            //单节点配置
            String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
            SingleServerConfig serverConfig = config.useSingleServer();
            serverConfig.setAddress(address);
            if (StringUtils.isNotBlank(redisProperties.getPassword())) {
                serverConfig.setPassword(redisProperties.getPassword());
            }
            serverConfig.setDatabase(redisProperties.getDatabase());
        }
        //看门狗的锁续期时间，默认30000ms，这里配置成15000ms
        config.setLockWatchdogTimeout(15000);
        this.redissonClient = Redisson.create(config);
        return this.redissonClient;
    }

    @Bean
    public ICacheService redissonCacheService(CacheProperties cacheProperties,
                                               RedissonClient redissonClient) {
        return new RedissonCacheServiceImpl(cacheProperties, redissonClient);
    }

    @Bean
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new RedissonLockService(redissonClient);
    }
}
