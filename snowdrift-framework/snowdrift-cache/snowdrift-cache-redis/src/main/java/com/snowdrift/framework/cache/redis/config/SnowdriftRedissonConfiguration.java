package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.redis.service.SnowdriftRedissonDistributedLockServiceImpl;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 分布式锁自动配置
 * <p>
 * {@link RedissonClient} 的连接构建与生命周期完全交给 <b>redisson-spring-boot-starter</b>
 * （读取 {@code spring.data.redis.*}、{@code redisson.yaml}、ACL、SSL Bundle 等），
 * 本类只通过 {@link RedissonAutoConfigurationCustomizer} 追加框架需要的差异配置：
 * {@link StringCodec} 与更短的看门狗续期时间。
 * </p>
 * <p>
 * 必须排在 starter 之后：{@code RedissonAutoConfigurationV2} 用
 * {@code @ConditionalOnMissingBean(RedissonClient.class)} 注册客户端，只有它先处理，
 * 定制器才能作用到实际创建的 {@code RedissonClient} 上。
 * </p>
 * <p>
 * 只要类路径存在 Redisson 就会生效（默认开启），可设 {@code snowdrift.cache.redisson.enabled=false}
 * 关闭本类：此时不再注册 {@link IDistributedLockService}、也不注入上述定制，但 starter 仍会创建
 * {@link RedissonClient}——需要彻底禁用请排除 starter 自身的自动配置。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(after = RedissonAutoConfigurationV2.class)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "snowdrift.cache.redisson", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class SnowdriftRedissonConfiguration {

    /**
     * 看门狗锁续期时间（毫秒）。
     * <p>Redisson 默认 30s，缩短至 15s 可更快释放失联客户端持有的锁。</p>
     */
    private static final long LOCK_WATCHDOG_TIMEOUT_MS = 15_000L;

    /**
     * 向 starter 创建的客户端追加定制配置
     * <p>
     * starter 会在构造 {@code Config} 后、{@code Redisson.create} 之前依次应用容器中所有
     * {@link RedissonAutoConfigurationCustomizer}，因此这里的定制才是实际生效的配置。
     * 使用 {@link StringCodec} 以 JSON 字符串存储，与 Caffeine / Redis 后端数据格式一致。
     * </p>
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            config.setCodec(StringCodec.INSTANCE);
            config.setLockWatchdogTimeout(LOCK_WATCHDOG_TIMEOUT_MS);
        };
    }

    @Bean
    @ConditionalOnMissingBean(IDistributedLockService.class)
    public IDistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new SnowdriftRedissonDistributedLockServiceImpl(redissonClient);
    }

}
