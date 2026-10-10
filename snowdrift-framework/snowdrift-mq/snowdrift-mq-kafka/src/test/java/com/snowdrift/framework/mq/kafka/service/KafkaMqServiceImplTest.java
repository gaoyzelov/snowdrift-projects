package com.snowdrift.framework.mq.kafka.service;

import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.properties.MqProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KafkaMqServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Kafka 发送实现黑盒测试：元数据映射、分区键/头写入、异常包装与透传、原生异步回调
 * @since 1.0.0
 */
class KafkaMqServiceImplTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
    @SuppressWarnings("unchecked")
    private final SendResult<String, byte[]> sendResult = mock(SendResult.class);
    private final RecordMetadata metadata = mock(RecordMetadata.class);

    @BeforeEach
    void setUp() {
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        MDC.clear();
    }

    private KafkaMqServiceImpl newService() {
        return new KafkaMqServiceImpl(kafkaTemplate, DIRECT_EXECUTOR, new FastJson2MqMessageConverter(),
                new MqInterceptorRegistry(), new MqContextPropagator(new MqProperties()));
    }

    private void stubMetadata(String topic, int partition, long offset, long timestamp) {
        when(metadata.topic()).thenReturn(topic);
        when(metadata.partition()).thenReturn(partition);
        when(metadata.offset()).thenReturn(offset);
        when(metadata.timestamp()).thenReturn(timestamp);
    }

    @SuppressWarnings("unchecked")
    private void stubSend(CompletableFuture<SendResult<String, byte[]>> future) {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
    }

    @Test
    @DisplayName("同步发送成功：messageId=topic-partition-offset，映射分区与 broker 时间戳")
    void syncSendMapsMetadata() {
        stubMetadata("order", 3, 42L, 1234L);
        stubSend(CompletableFuture.completedFuture(sendResult));
        KafkaMqServiceImpl service = newService();

        MqSendResult result = service.send("order", "k1", "payload");

        assertThat(result.getTopic()).isEqualTo("order");
        assertThat(result.getMessageId()).isEqualTo("order-3-42");
        assertThat(result.getPartitionOrQueue()).isEqualTo("3");
        assertThat(result.getTimestamp()).isEqualTo(1234L);
    }

    @Test
    @DisplayName("同步发送：key 作为分区键，上下文头写入记录头，消息体为序列化字节")
    @SuppressWarnings("unchecked")
    void syncSendBuildsRecordWithKeyAndHeaders() {
        stubMetadata("order", 0, 0L, 1L);
        stubSend(CompletableFuture.completedFuture(sendResult));
        KafkaMqServiceImpl service = newService();

        service.send("order", "k1", "payload", Map.of("biz", "pay"));

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, byte[]> record = captor.getValue();
        assertThat(record.topic()).isEqualTo("order");
        assertThat(record.key()).isEqualTo("k1");
        assertThat(new String(record.value())).contains("payload");
        assertThat(KafkaHeaders.headers(record)).containsEntry("biz", "pay");
    }

    @Test
    @DisplayName("边界：broker 时间戳为负（无时间戳）时回退到当前时间")
    void negativeTimestampFallsBackToNow() {
        stubMetadata("order", 1, 1L, -1L);
        stubSend(CompletableFuture.completedFuture(sendResult));
        KafkaMqServiceImpl service = newService();

        long before = System.currentTimeMillis();
        MqSendResult result = service.send("order", "payload");

        assertThat(result.getTimestamp()).isGreaterThanOrEqualTo(before);
    }

    @Test
    @DisplayName("同步发送失败：CompletionException 拆包后包装为 MqException 并保留原因")
    void syncSendFailureWrapped() {
        stubSend(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        KafkaMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.send("order", "payload"))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("Kafka 消息发送失败")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("同步发送失败：已是 MqException 时原样透传，不二次包装")
    void syncSendMqExceptionPassesThrough() {
        stubSend(CompletableFuture.failedFuture(new MqException("原始异常")));
        KafkaMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.send("order", "payload"))
                .isInstanceOf(MqException.class)
                .hasMessage("原始异常");
    }

    @Test
    @DisplayName("延迟发送：Kafka 无原生延迟能力，抛 UnsupportedOperationException")
    void sendDelayUnsupported() {
        KafkaMqServiceImpl service = newService();

        assertThatThrownBy(() -> service.sendDelay("order", "payload", java.time.Duration.ofSeconds(1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("原生异步发送成功：future 完成并携带映射后的结果")
    void asyncSendSuccess() {
        stubMetadata("order", 5, 99L, 2000L);
        stubSend(CompletableFuture.completedFuture(sendResult));
        KafkaMqServiceImpl service = newService();

        CompletableFuture<MqSendResult> future = service.sendAsync("order", "k", "payload");

        assertThat(future.join().getMessageId()).isEqualTo("order-5-99");
    }

    @Test
    @DisplayName("原生异步发送失败：future 以 MqException 异常完成")
    void asyncSendFailure() {
        stubSend(CompletableFuture.failedFuture(new IllegalStateException("io error")));
        KafkaMqServiceImpl service = newService();

        CompletableFuture<MqSendResult> future = service.sendAsync("order", "k", "payload");

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(MqException.class);
    }

    /** 读取记录头的测试辅助 */
    private static final class KafkaHeaders {
        static Map<String, String> headers(ProducerRecord<String, byte[]> record) {
            return com.snowdrift.framework.mq.kafka.context.KafkaHeaderCodec.toMap(record.headers());
        }
    }
}
