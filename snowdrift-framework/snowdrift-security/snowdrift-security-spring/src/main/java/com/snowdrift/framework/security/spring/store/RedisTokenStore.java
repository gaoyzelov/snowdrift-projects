package com.snowdrift.framework.security.spring.store;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.common.util.DesensitizeUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis 的 {@link TokenStore} 实现
 * <p>
 * Key 格式：{@code <headerName>:token:<token>}（与 Sa-Token 命名风格一致），支持 TTL 自动过期。
 * SecurityContext 使用 FastJson2 序列化为 JSON 存储。
 * 当容器中存在 {@link StringRedisTemplate} Bean 时自动替代 InMemory 实现。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class RedisTokenStore implements TokenStore {

    private final String keyPrefix;
    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate, String headerName) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = headerName + ":token:";
    }

    @Override
    public void put(String token, SecurityContext context, long timeout) {
        String key = buildKey(token);
        String value = JSON.toJSONString(context);
        if (timeout > 0) {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout));
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
        log.trace("Redis TokenStore 写入: token={}, expireIn={}s", DesensitizeUtil.process(token, "(?<=.{8}).+", "***"), timeout);
    }

    @Override
    public SecurityContext get(String token) {
        String key = buildKey(token);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        return JSON.parseObject(json, SecurityContext.class);
    }

    @Override
    public void remove(String token) {
        String key = buildKey(token);
        redisTemplate.delete(key);
        log.trace("Redis TokenStore 移除: token={}", DesensitizeUtil.process(token, "(?<=.{8}).+", "***"));
    }

    private String buildKey(String token) {
        return keyPrefix + token;
    }

}
