package com.snowdrift.cache.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.snowdrift.cache.redis.service.IRedisCacheService;
import com.snowdrift.cache.redis.service.impl.RedisCacheServiceImpl;
import com.snowdrift.core.constant.StrConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * RedisCacheAutoConfiguration
 *
 * @author gaoye
 * @date 2025/03/20 10:58:32
 * @description xxxxxxxx
 * @since 1.0
 */
@Configuration
@EnableCaching
public class RedisCacheAutoConfiguration extends CachingConfigurerSupport {

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 设置key和value的序列化方式
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(jackson2JsonRedisSerializer());
        // 设置hash的key和value的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(jackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置缓存,反参格式,前缀及过期时间
     *
     * @param factory 连接工厂
     * @return CacheManager
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1L))
                //变双冒号为单冒号
                .computePrefixWith(name -> name.replace(StrConst.DOUBLE_COLON, StrConst.COLON))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                .disableCachingNullValues();
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(factory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    /**
     * Jackson 序列化器
     */
    private Jackson2JsonRedisSerializer<?> jackson2JsonRedisSerializer() {
        //jackson进行序列化和反序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String,Integer等会跑出异常
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        //LocalDateTime类型redis序列化、反序列化异常处理
        om.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        om.registerModule(new JavaTimeModule());
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

    /**
     * 缓存key默认生成规则
     * 类名+方法名
     */
    @Override
    public KeyGenerator keyGenerator() {
        return (o, method, objects) -> o.getClass().getName() + StrConst.DOT + method.getName();
    }

    /**
     * 缓存服务
     *
     * @param redisTemplate redisTemplate
     * @return IRedisCacheService
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public IRedisCacheService redisCacheService(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheServiceImpl(redisTemplate);
    }
}