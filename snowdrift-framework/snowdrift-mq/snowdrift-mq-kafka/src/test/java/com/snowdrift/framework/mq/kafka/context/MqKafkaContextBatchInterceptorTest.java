package com.snowdrift.framework.mq.kafka.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * MqKafkaContextBatchInterceptorTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Kafka 批消费上下文拦截器黑盒测试：批前/批后（成功、失败）均清理，且刻意不还原上下文
 * @since 1.0.0
 */
class MqKafkaContextBatchInterceptorTest {

    @SuppressWarnings("unchecked")
    private final Consumer<String, byte[]> consumer = mock(Consumer.class);
    private final MqContextPropagator propagator = mock(MqContextPropagator.class);
    private final MqKafkaContextBatchInterceptor interceptor = new MqKafkaContextBatchInterceptor(propagator);

    private final ConsumerRecords<String, byte[]> records = new ConsumerRecords<>(Map.of());

    @Test
    @DisplayName("intercept：批开始前清理上下文并原样返回批次，不做还原")
    void interceptClearsWithoutRestore() {
        ConsumerRecords<String, byte[]> returned = interceptor.intercept(records, consumer);

        assertThat(returned).isSameAs(records);
        verify(propagator).clear();
        verify(propagator, never()).restore(any());
    }

    @Test
    @DisplayName("success/failure 均清理上下文")
    void successAndFailureClear() {
        interceptor.success(records, consumer);
        interceptor.failure(records, new RuntimeException("x"), consumer);

        verify(propagator, times(2)).clear();
    }
}
