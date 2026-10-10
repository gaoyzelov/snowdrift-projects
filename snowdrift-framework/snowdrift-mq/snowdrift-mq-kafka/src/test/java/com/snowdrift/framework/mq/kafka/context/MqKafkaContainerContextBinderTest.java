package com.snowdrift.framework.mq.kafka.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.CompositeRecordInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * MqKafkaContainerContextBinderTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Kafka 容器工厂绑定器黑盒测试：挂载记录级/批级拦截器、与既有拦截器合并、非目标 Bean 原样返回
 * @since 1.0.0
 */
class MqKafkaContainerContextBinderTest {

    private final MqKafkaContextRecordInterceptor recordInterceptor =
            new MqKafkaContextRecordInterceptor(mock(MqContextPropagator.class));
    private final MqKafkaContextBatchInterceptor batchInterceptor =
            new MqKafkaContextBatchInterceptor(mock(MqContextPropagator.class));
    private final MqKafkaContainerContextBinder binder =
            new MqKafkaContainerContextBinder(recordInterceptor, batchInterceptor);

    private static Object readField(AbstractKafkaListenerContainerFactory<?, ?, ?> factory, String name) throws Exception {
        Field field = AbstractKafkaListenerContainerFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(factory);
    }

    @Test
    @DisplayName("无既有拦截器时直接挂载本框架记录级与批级拦截器")
    void bindsInterceptorsWhenNoneExisting() throws Exception {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();

        Object returned = binder.postProcessAfterInitialization(factory, "factory");

        assertThat(returned).isSameAs(factory);
        assertThat(readField(factory, "recordInterceptor")).isSameAs(recordInterceptor);
        assertThat(readField(factory, "batchInterceptor")).isSameAs(batchInterceptor);
    }

    @Test
    @DisplayName("已有记录级拦截器时组合为 CompositeRecordInterceptor，不整体覆盖")
    @SuppressWarnings("unchecked")
    void mergesWithExistingRecordInterceptor() throws Exception {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        RecordInterceptor<String, byte[]> existing = mock(RecordInterceptor.class);
        factory.setRecordInterceptor(existing);

        binder.postProcessAfterInitialization(factory, "factory");

        assertThat(readField(factory, "recordInterceptor")).isInstanceOf(CompositeRecordInterceptor.class);
    }

    @Test
    @DisplayName("非容器工厂 Bean 原样返回，不做处理")
    void ignoresNonTargetBean() {
        Object other = new Object();

        assertThat(binder.postProcessAfterInitialization(other, "other")).isSameAs(other);
    }
}
