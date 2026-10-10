package com.snowdrift.framework.mq.kafka.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.exception.MqException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * MqKafkaContextRecordInterceptorTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Kafka 记录级上下文拦截器黑盒测试：消费前从头恢复、各生命周期回调均清理、签名失败中止
 * @since 1.0.0
 */
class MqKafkaContextRecordInterceptorTest {

    @SuppressWarnings("unchecked")
    private final Consumer<String, byte[]> consumer = mock(Consumer.class);
    private final MqContextPropagator propagator = mock(MqContextPropagator.class);
    private final MqKafkaContextRecordInterceptor interceptor = new MqKafkaContextRecordInterceptor(propagator);

    private ConsumerRecord<String, byte[]> recordWithHeaders() {
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("topic", 0, 0L, "k", "v".getBytes(StandardCharsets.UTF_8));
        record.headers().add(new RecordHeader("x-snowdrift-trace-id", "trace-1".getBytes(StandardCharsets.UTF_8)));
        return record;
    }

    @Test
    @DisplayName("intercept：从记录头恢复上下文并原样返回记录")
    @SuppressWarnings("unchecked")
    void interceptRestoresFromHeaders() {
        ConsumerRecord<String, byte[]> record = recordWithHeaders();

        ConsumerRecord<String, byte[]> returned = interceptor.intercept(record, consumer);

        assertThat(returned).isSameAs(record);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(propagator).restore(captor.capture());
        assertThat(captor.getValue()).containsEntry("x-snowdrift-trace-id", "trace-1");
    }

    @Test
    @DisplayName("success/failure/afterRecord 均清理上下文")
    void lifecycleCallbacksClear() {
        ConsumerRecord<String, byte[]> record = recordWithHeaders();

        interceptor.success(record, consumer);
        interceptor.failure(record, new RuntimeException("x"), consumer);
        interceptor.afterRecord(record, consumer);

        verify(propagator, org.mockito.Mockito.times(3)).clear();
    }

    @Test
    @DisplayName("签名校验失败：restore 抛异常则向上抛出以中止消费")
    void restoreFailurePropagates() {
        doThrow(new MqException("消息签名校验失败，已拒绝消费该消息")).when(propagator).restore(any());

        assertThatThrownBy(() -> interceptor.intercept(recordWithHeaders(), consumer))
                .isInstanceOf(MqException.class);
    }
}
