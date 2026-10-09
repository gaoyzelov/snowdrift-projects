package com.snowdrift.framework.cache.redis.config;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.cache.ICacheService;
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
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
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
     * Spring Cache 管理器（供 {@code @Cacheable} 使用，JSON 序列化、统一前缀 + TTL）
     * <p>
     * <b>本 Bean 不加 {@code @ConditionalOnMissingBean} 是有意为之</b>：它需要优先于 Boot
     * {@code CacheAutoConfiguration} 自动注册的默认 {@code CacheManager}（Redis 场景下 Boot 的默认实现
     * 使用 JDK 序列化与默认配置）。若给它加上该条件，一旦 Boot 的配置先生效，本 Bean 便会静默退避，
     * 使 {@code snowdrift.cache.*} 配置的序列化器、key 前缀与 TTL <b>全部悄无声息地失效</b>——
     * 这是 fail-silent，比直接报错更难发现。
     * </p>
     * <p>
     * 应用若需使用自定义 {@code CacheManager}，请给自身 Bean 标注 {@code @Primary}，
     * 或用 {@code spring.autoconfigure.exclude} 排除本自动配置，<b>不要依赖本 Bean 自动退避</b>；
     * 否则容器内会同时存在两个 {@code CacheManager}——<b>启动期</b>就会因
     * {@code NoUniqueBeanDefinitionException} 失败（Spring 在初始化缓存基础设施时即需选定唯一的
     * {@code CacheManager}，不会等到 {@code @Cacheable} 调用时才报错）。
     * </p>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        // Spring Cache 的 RedisSerializer 拿不到方法返回类型，必须用带类型信息的 mapper（写 @class），
        // 否则反序列化得到 LinkedHashMap、调用方强转时抛 ClassCastException。
        // 这里**不随 snowdrift.cache.serializer 切换**：Jackson 的 @class 与 fastjson2 的 @type 两种类型元数据
        // 互不可读，若随配置切换，改一次配置就会让既有 @Cacheable 缓存整体读不出来；
        // 该配置只影响 ICacheService 的序列化器（两个实现已对齐，值格式一致）。
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
