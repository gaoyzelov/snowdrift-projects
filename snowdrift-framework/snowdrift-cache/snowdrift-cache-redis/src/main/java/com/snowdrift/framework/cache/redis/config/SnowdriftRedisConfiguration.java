package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.cache.serialize.CacheSerializer;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.CacheProperties;
import com.snowdrift.framework.cache.redis.service.RedisCacheServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 缓存自动配置
 * <p>
 * 当 Spring Data Redis 可用且 {@link RedisConnectionFactory} 存在时激活。
 * 使用 JSON 字符串存储，由 {@link CacheSerializer} 统一处理序列化，
 * 与 Caffeine / Redisson 后端数据格式一致。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
public class SnowdriftRedisConfiguration {

    private final CacheProperties cacheProperties;

    private final RedisConnectionFactory connectionFactory;

    public SnowdriftRedisConfiguration(CacheProperties cacheProperties,
                                        RedisConnectionFactory connectionFactory) {
        this.cacheProperties = cacheProperties;
        this.connectionFactory = connectionFactory;
    }

    /**
     * String-String RedisTemplate，序列化由 {@link CacheSerializer} 在服务层统一处理
     */
    @Bean(name = "stringRedisTemplate")
    @ConditionalOnMissingBean(name = "stringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.string());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(ICacheService.class)
    public ICacheService redisCacheService(CacheSerializer serializer,
                                            RedisTemplate<String, String> stringRedisTemplate) {
        return new RedisCacheServiceImpl(cacheProperties, serializer, stringRedisTemplate);
    }
}
