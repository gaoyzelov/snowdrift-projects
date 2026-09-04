package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.SerializerType;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.redis.serialize.FastJson2RedisSerializer;
import com.snowdrift.framework.cache.redis.service.SnowdriftRedisCacheServiceImpl;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 缓存自动配置
 * <p>
 * 当 Spring Data Redis 可用且 {@link RedisConnectionFactory} 存在时激活。
 * 使用 JSON 字符串存储，由 {@link ICacheSerializer} 统一处理序列化，
 * 与 Caffeine / Redisson 后端数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnMissingBean(ICacheService.class)
public class SnowdriftRedisConfiguration {

    private final SnowdriftCacheProperties properties;

    private final RedisConnectionFactory factory;

    public SnowdriftRedisConfiguration(SnowdriftCacheProperties properties, RedisConnectionFactory factory) {
        this.properties = properties;
        this.factory = factory;
    }

    /**
     * String-String RedisTemplate，序列化由 {@link ICacheSerializer} 在服务层统一处理
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.string());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Spring Cache 管理器（供 @Cacheable 注解使用，JSON 序列化，统一前缀+TTL）
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer;
        if (properties.getSerializer() == SerializerType.FASTJSON2) {
            valueSerializer = new FastJson2RedisSerializer();
        } else {
            valueSerializer = new Jackson2JsonRedisSerializer<>(JacksonCacheSerializer.defaultMapper(), Object.class);
        }
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置key序列化器：String
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                // value序列化：Jackson JSON
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                // key前缀，prefix:cacheName:key
                .computePrefixWith(name -> properties.getKeyPrefix() + StrConst.COLON + name + StrConst.COLON)
                // 全局缓存过期时间
                .entryTtl(properties.getKeyTtl());

        return RedisCacheManager.builder(connectionFactory)
                // 默认配置
                .cacheDefaults(config)
                // 开启事务感知
                .transactionAware()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ICacheService.class)
    public ICacheService redisCacheService(ICacheSerializer serializer, RedisTemplate<String, String> redisTemplate) {
        return new SnowdriftRedisCacheServiceImpl(properties, serializer, redisTemplate);
    }
}
