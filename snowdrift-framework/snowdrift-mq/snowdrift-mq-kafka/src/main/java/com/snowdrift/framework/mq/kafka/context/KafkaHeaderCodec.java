package com.snowdrift.framework.mq.kafka.context;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka 原生 Headers 与 broker 无关的 {@code Map<String,String>} 消息头互转
 * <p>值统一按 UTF-8 编码为字节。发送端写、消费端读（供 {@link MqKafkaContextRecordInterceptor} 恢复上下文）。</p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
public final class KafkaHeaderCodec {

    private KafkaHeaderCodec() {
    }

    /**
     * 把 broker 无关头写入 Kafka 记录头
     */
    public static void apply(Map<String, String> headers, ProducerRecord<String, byte[]> record) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : new byte[0];
            record.headers().add(new RecordHeader(key, bytes));
        });
    }

    /**
     * 把 Kafka 记录头读取为 broker 无关头 Map（仅含可解码为 UTF-8 的字符串头）
     */
    public static Map<String, String> toMap(Headers headers) {
        Map<String, String> result = new LinkedHashMap<>();
        if (headers == null) {
            return result;
        }
        for (Header header : headers) {
            if (header.key() == null) {
                continue;
            }
            byte[] value = header.value();
            if (value != null) {
                result.put(header.key(), new String(value, StandardCharsets.UTF_8));
            }
        }
        return result;
    }
}
