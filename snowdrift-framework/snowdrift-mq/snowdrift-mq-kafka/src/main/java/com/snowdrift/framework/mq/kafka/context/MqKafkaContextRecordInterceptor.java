package com.snowdrift.framework.mq.kafka.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

import java.util.Map;

/**
 * Kafka 消费上下文自动恢复拦截器
 * <p>
 * 挂在 {@code @KafkaListener} 监听容器上：每个记录消费前从 Kafka 头恢复 traceId / SecurityContext，
 * 消费结束（成功/失败）后清理，业务监听方法内无需任何样板代码。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
public class MqKafkaContextRecordInterceptor implements RecordInterceptor<String, byte[]> {

    private final MqContextPropagator contextPropagator;

    public MqKafkaContextRecordInterceptor(MqContextPropagator contextPropagator) {
        this.contextPropagator = contextPropagator;
    }

    @Override
    public ConsumerRecord<String, byte[]> intercept(ConsumerRecord<String, byte[]> record, Consumer<String, byte[]> consumer) {
        // 签名不通过时 restore 会抛异常并已自行 clear：异常向上抛出以中止消费并交给监听容器的错误处理
        Map<String, String> headers = KafkaHeaderCodec.toMap(record.headers());
        contextPropagator.restore(headers);
        return record;
    }

    @Override
    public void success(ConsumerRecord<String, byte[]> record, Consumer<String, byte[]> consumer) {
        contextPropagator.clear();
    }

    @Override
    public void failure(ConsumerRecord<String, byte[]> record, Exception exception, Consumer<String, byte[]> consumer) {
        contextPropagator.clear();
    }

    @Override
    public void afterRecord(ConsumerRecord<String, byte[]> record, Consumer<String, byte[]> consumer) {
        contextPropagator.clear();
    }
}
