package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisTokenStoreTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Redis TokenStore 黑盒测试：key 前缀拼接、TTL 有无分支、读取类型校验、touch 续期与移除
 * @since 1.0.0
 */
class RedisTokenStoreTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private RedisTokenStore store(long timeout, long idle) {
        return new RedisTokenStore(redisTemplate, timeout, idle, "pfx");
    }

    private static SecurityContext ctx(long id) {
        return SecurityContext.builder().userId(id).build();
    }

    @Test
    @DisplayName("put 带正向 TTL：以 prefix:token 为 key、携带 Duration 写入")
    void putWithTtl() {
        RedisTokenStore store = store(3600, 1800);

        store.put("tok", ctx(1L), Duration.ofSeconds(60));

        ArgumentCaptor<AbstractTokenStore.TokenEntry> captor =
                ArgumentCaptor.forClass(AbstractTokenStore.TokenEntry.class);
        verify(valueOps).set(eq("pfx:tok"), captor.capture(), eq(Duration.ofSeconds(60)));
        assertThat(captor.getValue().getContext().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("put 永不过期（ttl<=0）：写入但不设置 Redis TTL")
    void putWithoutTtlWhenNeverExpire() {
        RedisTokenStore store = store(-1, -1);

        store.put("tok", ctx(2L), null);

        verify(valueOps).set(eq("pfx:tok"), any(AbstractTokenStore.TokenEntry.class));
        verify(valueOps, never()).set(any(String.class), any(), any(Duration.class));
    }

    @Test
    @DisplayName("get：命中未过期 TokenEntry 返回上下文，并触发 touch 续期写回")
    void getReturnsContextAndTouches() {
        RedisTokenStore store = store(-1, -1);
        AbstractTokenStore.TokenEntry entry =
                new AbstractTokenStore.TokenEntry(ctx(3L), Long.MAX_VALUE, System.currentTimeMillis());
        when(valueOps.get("pfx:tok")).thenReturn(entry);

        SecurityContext result = store.get("tok");

        assertThat(result.getUserId()).isEqualTo(3L);
        // 永不过期分支：touch 以无 TTL 方式写回
        verify(valueOps).set(eq("pfx:tok"), any(AbstractTokenStore.TokenEntry.class));
    }

    @Test
    @DisplayName("get：值类型不是 TokenEntry（脏数据）时返回 null")
    void getReturnsNullOnUnexpectedType() {
        RedisTokenStore store = store(3600, 1800);
        when(valueOps.get("pfx:tok")).thenReturn("not-a-token-entry");

        assertThat(store.get("tok")).isNull();
    }

    @Test
    @DisplayName("get：key 不存在返回 null")
    void getReturnsNullWhenAbsent() {
        RedisTokenStore store = store(3600, 1800);
        when(valueOps.get("pfx:tok")).thenReturn(null);

        assertThat(store.get("tok")).isNull();
    }

    @Test
    @DisplayName("get：绝对+闲置均启用时，touch 以带 TTL 方式续期")
    void touchUsesTtlWhenExpiryEnabled() {
        RedisTokenStore store = store(3600, 1800);
        AbstractTokenStore.TokenEntry entry = new AbstractTokenStore.TokenEntry(
                ctx(4L), System.currentTimeMillis() + 3_600_000L, System.currentTimeMillis());
        when(valueOps.get("pfx:tok")).thenReturn(entry);

        assertThat(store.get("tok")).isNotNull();

        verify(valueOps).set(eq("pfx:tok"), any(AbstractTokenStore.TokenEntry.class), any(Duration.class));
    }

    @Test
    @DisplayName("remove：按 prefix:token 删除 key")
    void removeDeletesKey() {
        RedisTokenStore store = store(3600, 1800);

        store.remove("tok");

        verify(redisTemplate).delete("pfx:tok");
    }
}
