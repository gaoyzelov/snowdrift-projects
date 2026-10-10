package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.config.SnowdriftCacheConfiguration;
import com.snowdrift.framework.cache.properties.SnowdriftCacheProperties;
import com.snowdrift.framework.cache.redis.service.SnowdriftRedisCacheServiceImpl;
import com.snowdrift.framework.cache.serialize.ICacheSerializer;
import com.snowdrift.framework.cache.serialize.JacksonCacheSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
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
 * <p>
 * <b>本类的条件只作用于 {@link RedisConnectionFactory} 与各 Bean 自身</b>：不要在这里加类级
 * {@code @ConditionalOnMissingBean(ICacheService.class)}——那会让业务自定义一个 {@code ICacheService}
 * 就顺带关掉 {@code CacheManager}、{@code snowdriftRedisTemplate} 与 key 前缀/TTL 配置，
 * {@code @Cacheable} 会静默退化成 Boot 默认缓存，属最难排查的一类失效。
 * </p>
 * <p>
 * <b>为何必须 {@code before = SnowdriftCacheConfiguration.class}：</b>{@link SnowdriftCacheConfiguration}
 * 里的切面用 {@code @ConditionalOnBean(ICacheService.class)} 判断后端是否就绪，该条件在<b>解析顺序</b>上
 * 只能看到"先处理过的"配置类所注册的 Bean。不显式排在本类之前时，按类名排序
 * （{@code cache.config.*} &lt; {@code cache.redis.*}）本类会排在核心配置<b>之后</b>，
 * 于是 {@code RepeatSubmitAspect} / {@code DistributedLockAspect} 静默不注册——
 * {@code @RepeatSubmit}、{@code @DistributedLock} 全部失效且不报错（已实测：仅核心配置 + 本类时两个切面均为 0 个）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(after = RedisAutoConfiguration.class, before = SnowdriftCacheConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
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
    @Bean(name = "snowdriftRedisTemplate")
    public RedisTemplate<String, String> snowdriftRedisTemplate() {
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
     * Spring Cache 管理器
     * </p>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(JacksonCacheSerializer.cacheValueMapper(), Object.class);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置key序列化器：String
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                // value序列化：Jackson JSON
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                // key前缀，prefix:cacheName:key（未配置前缀时省略前缀）
                .computePrefixWith(name -> StringUtils.isBlank(properties.getKeyPrefix())
                        ? name + StrConst.COLON
                        : properties.getKeyPrefix() + StrConst.COLON + name + StrConst.COLON)
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
    public ICacheService redisCacheService(ICacheSerializer serializer,
                                          @Qualifier("snowdriftRedisTemplate") RedisTemplate<String, String> snowdriftRedisTemplate) {
        return new SnowdriftRedisCacheServiceImpl(properties, serializer, snowdriftRedisTemplate);
    }
}
