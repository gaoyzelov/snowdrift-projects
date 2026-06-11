package com.snowdrift.framework.cache.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowdrift.framework.cache.CacheSerializer;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.cache.redis.service.RedisCacheServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 缓存自动配置
 * <p>
 * 当 Spring Data Redis 可用且 {@link RedisConnectionFactory} 存在时激活。
 * 实现 {@link CachingConfigurer} 提供自定义 {@link CacheManager}，
 * 与 ICacheService 共享同一套 Jackson 序列化机制。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnMissingBean(ICacheService.class)
public class SnowdriftRedisConfiguration implements CachingConfigurer {

    private final CacheProperties cacheProperties;

    private final RedisConnectionFactory connectionFactory;

    public SnowdriftRedisConfiguration(CacheProperties cacheProperties,
                                        RedisConnectionFactory connectionFactory) {
        this.cacheProperties = cacheProperties;
        this.connectionFactory = connectionFactory;
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(jackson2JsonRedisSerializer());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(jackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Override
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getDefaultTtl())
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public ICacheService redisCacheService(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheServiceImpl(cacheProperties, redisTemplate);
    }

    /**
     * 基于共享 ObjectMapper 创建 Jackson2JsonRedisSerializer
     */
    private static Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        ObjectMapper om = CacheSerializer.getObjectMapper();
        return new Jackson2JsonRedisSerializer<>(om, Object.class);
    }
}
