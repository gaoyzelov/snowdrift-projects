package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 缓存类型枚举
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CacheTypeEnum implements IEnum<String> {

    /**
     * Caffeine 本地缓存
     */
    CAFFEINE("caffeine", "Caffeine 本地缓存"),

    /**
     * Redis 缓存（基于 RedisTemplate，jedis/lettuce 连接）
     */
    REDIS("redis", "Redis 缓存"),

    /**
     * Redisson 缓存（支持分布式锁、RMap 等高级特性）
     */
    REDISSON("redisson", "Redisson 缓存");

    private final String code;

    private final String note;

    /**
     * 根据 code 获取枚举
     *
     * @param code 枚举值
     * @return 枚举对象
     */
    public static Optional<CacheTypeEnum> getByCode(String code) {
        return IEnum.getByCode(CacheTypeEnum.class, code);
    }
}
