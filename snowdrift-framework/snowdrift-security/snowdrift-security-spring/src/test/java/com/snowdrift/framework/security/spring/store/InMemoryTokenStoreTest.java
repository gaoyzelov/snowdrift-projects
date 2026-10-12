package com.snowdrift.framework.security.spring.store;

import com.snowdrift.framework.context.security.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryTokenStoreTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 内存 TokenStore（含 AbstractTokenStore 模板逻辑）黑盒测试：读写/移除、永不过期、绝对过期、
 * 闲置过期、活跃刷新，以及「get 交付副本、不交出存储内实例」的隔离契约
 * @since 1.0.0
 */
class InMemoryTokenStoreTest {

    private static SecurityContext ctx(long userId) {
        return SecurityContext.builder().userId(userId).username("u" + userId).build();
    }

    @Test
    @DisplayName("put/get 往返返回同一上下文；未知 token 返回 null")
    void putGetRoundTrip() {
        InMemoryTokenStore store = new InMemoryTokenStore(3600, 1800);
        SecurityContext context = ctx(1L);

        store.put("tok-1", context, null);

        assertThat(store.get("tok-1")).isEqualTo(context).isNotSameAs(context);
        assertThat(store.get("unknown")).isNull();
    }

    @Test
    @DisplayName("remove 后不可再读取")
    void removeEvicts() {
        InMemoryTokenStore store = new InMemoryTokenStore(3600, 1800);
        store.put("tok", ctx(1L), null);

        store.remove("tok");

        assertThat(store.get("tok")).isNull();
    }

    @Test
    @DisplayName("timeout<=0 表示永不过期：不做绝对过期淘汰")
    void neverExpire() {
        InMemoryTokenStore store = new InMemoryTokenStore(-1, -1);
        SecurityContext context = ctx(2L);

        store.put("tok", context, null);

        assertThat(store.get("tok")).isEqualTo(context).isNotSameAs(context);
    }

    @Test
    @DisplayName("显式传入的正向 timeout 生效，未过期时可读取")
    void explicitTimeoutNotYetExpired() {
        InMemoryTokenStore store = new InMemoryTokenStore(3600, 1800);
        SecurityContext context = ctx(3L);

        store.put("tok", context, Duration.ofSeconds(60));

        assertThat(store.get("tok")).isEqualTo(context).isNotSameAs(context);
    }

    @Test
    @DisplayName("绝对过期：超过 timeout 后 get 返回 null 并移除")
    void absoluteExpiry() throws InterruptedException {
        InMemoryTokenStore store = new InMemoryTokenStore(1, 3600);
        store.put("tok", ctx(4L), null);

        Thread.sleep(1300);

        assertThat(store.get("tok")).isNull();
    }

    @Test
    @DisplayName("闲置过期：超过 idle 未活跃后 get 返回 null")
    void idleExpiry() throws InterruptedException {
        // 绝对超时很长，闲置超时 1s
        InMemoryTokenStore store = new InMemoryTokenStore(3600, 1);
        store.put("tok", ctx(5L), null);

        Thread.sleep(1300);

        assertThat(store.get("tok")).isNull();
    }

    @Test
    @DisplayName("安全边界：get 返回副本，业务 setter 不得污染会话存储")
    void getReturnsDefensiveCopy() {
        InMemoryTokenStore store = new InMemoryTokenStore(3600, 1800);
        SecurityContext stored = SecurityContext.builder()
                .userId(1L).username("alice").nickname("爱丽丝")
                .tenantId(11L).deptId(22L).platform("web")
                .roleKeys(new ArrayList<>(List.of("admin")))
                .permissions(new ArrayList<>(List.of("sys:user:list")))
                .attributes(new HashMap<>(Map.of("token", "raw-secret")))
                .build();
        store.put("tok", stored, null);

        SecurityContext first = store.get("tok");
        assertThat(first).isNotSameAs(stored);

        // 模拟业务代码对取到的上下文做任意修改
        first.setUserId(999L);
        first.setTenantId(999L);
        first.getRoleKeys().add("hacked");
        first.getPermissions().clear();
        first.getAttributes().put("injected", "x");

        SecurityContext second = store.get("tok");
        assertThat(second.getUserId()).isEqualTo(1L);
        assertThat(second.getTenantId()).isEqualTo(11L);
        assertThat(second.getRoleKeys()).containsExactly("admin");
        assertThat(second.getPermissions()).containsExactly("sys:user:list");
        assertThat(second.getAttributes()).containsOnlyKeys("token");
    }

    @Test
    @DisplayName("活跃刷新：闲置窗口内多次 get 不会因闲置过期")
    void touchRefreshesIdleWindow() throws InterruptedException {
        InMemoryTokenStore store = new InMemoryTokenStore(3600, 1);
        SecurityContext context = ctx(6L);
        store.put("tok", context, null);

        // 每 500ms 访问一次，累计 1.5s 仍活跃（闲置窗口 1s 被不断刷新）
        for (int i = 0; i < 3; i++) {
            Thread.sleep(500);
            assertThat(store.get("tok")).isEqualTo(context).isNotSameAs(context);
        }
    }
}
