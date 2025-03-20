package com.snowdrift.cache.redis.config;

import com.snowdrift.cache.redis.aspect.DistLockAspect;
import com.snowdrift.cache.redis.service.IRedisLockService;
import com.snowdrift.cache.redis.service.impl.RedisLockServiceImpl;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RedisLockAutoConfiguration
 *
 * @author gaoye
 * @date 2025/03/20 10:19:49
 * @description Redis 分布式锁配置
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(RedissonAutoConfiguration.class)
@Import(DistLockAspect.class)
@ConditionalOnBean(RedissonClient.class)
public class RedisLockAutoConfiguration {

    /**
     * Redis 分布式锁服务
     * @return IRedisLockService
     */
    @Bean
    public IRedisLockService redisLockService(RedissonClient redissonClient){
        return new RedisLockServiceImpl(redissonClient);
    }
}