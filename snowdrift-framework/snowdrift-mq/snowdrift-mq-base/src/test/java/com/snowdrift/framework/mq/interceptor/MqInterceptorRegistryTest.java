package com.snowdrift.framework.mq.interceptor;

import com.snowdrift.framework.mq.model.MqSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MqInterceptorRegistryTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 拦截器注册表单元测试：优先级降序、同优先级注册序稳定、动态增删、null 安全与快照隔离
 * @since 1.0.0
 */
class MqInterceptorRegistryTest {

    /** 具名拦截器，仅用于断言顺序 */
    static class NamedInterceptor implements MqSendInterceptor {
        private final String name;
        private final int priority;

        NamedInterceptor(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static List<String> names(List<MqSendInterceptor> interceptors) {
        return interceptors.stream().map(Object::toString).toList();
    }

    @Test
    @DisplayName("按优先级降序返回")
    void sortsByPriorityDescending() {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        registry.register(new NamedInterceptor("low", 0));
        registry.register(new NamedInterceptor("high", 100));
        registry.register(new NamedInterceptor("mid", 50));

        assertThat(names(registry.getInterceptors())).containsExactly("high", "mid", "low");
    }

    @Test
    @DisplayName("同优先级保持注册先后（稳定排序）")
    void keepsRegistrationOrderForEqualPriority() {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        registry.register(new NamedInterceptor("first", 10));
        registry.register(new NamedInterceptor("second", 10));
        registry.register(new NamedInterceptor("third", 10));

        assertThat(names(registry.getInterceptors())).containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("默认优先级为 0")
    void defaultPriorityIsZero() {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        registry.register(new MqSendInterceptor() {
            @Override
            public String toString() {
                return "default";
            }
        });
        registry.register(new NamedInterceptor("explicit", 1));

        assertThat(names(registry.getInterceptors())).containsExactly("explicit", "default");
    }

    @Test
    @DisplayName("unregister 移除指定拦截器")
    void unregisterRemoves() {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        MqSendInterceptor a = new NamedInterceptor("a", 1);
        MqSendInterceptor b = new NamedInterceptor("b", 2);
        registry.register(a);
        registry.register(b);

        registry.unregister(a);

        assertThat(names(registry.getInterceptors())).containsExactly("b");
    }

    @Test
    @DisplayName("null 安全：register(null)/unregister(null) 不抛异常且不改变集合")
    void nullSafe() {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        registry.register(new NamedInterceptor("a", 1));

        registry.register(null);
        registry.unregister(null);

        assertThat(names(registry.getInterceptors())).containsExactly("a");
    }

    @Test
    @DisplayName("空注册表返回空列表")
    void emptyRegistry() {
        assertThat(new MqInterceptorRegistry().getInterceptors()).isEmpty();
    }

    @Test
    @DisplayName("快照隔离：修改返回列表不影响注册表内部状态")
    void snapshotIsolation() {
        MqInterceptorRegistry registry = new MqInterceptorRegistry();
        registry.register(new NamedInterceptor("a", 1));

        List<MqSendInterceptor> snapshot = registry.getInterceptors();
        snapshot.clear();

        assertThat(registry.getInterceptors()).hasSize(1);
    }

    @Test
    @DisplayName("SPI 默认回调为空实现，调用不抛异常")
    void defaultCallbacksAreNoOp() {
        MqSendInterceptor interceptor = new MqSendInterceptor() {
        };
        interceptor.beforeSend("t", "k", "payload");
        interceptor.afterSend("t", MqSendResult.builder().topic("t").build());
        interceptor.onSendError("t", new RuntimeException("x"));

        assertThat(interceptor.getPriority()).isZero();
    }
}
