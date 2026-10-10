package com.snowdrift.framework.mq.kafka.context;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * KafkaHeaderCodecTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Kafka 原生 Headers 与 broker 无关 Map 互转单元测试：往返对称、null 安全与 UTF-8 编码
 * @since 1.0.0
 */
class KafkaHeaderCodecTest {

    @Test
    @DisplayName("apply→toMap 往返对称还原字符串头")
    void roundTrip() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-snowdrift-trace-id", "trace-1");
        headers.put("x-snowdrift-username", "张三");
        ProducerRecord<String, byte[]> record = new ProducerRecord<>("topic", "key", new byte[]{1});

        KafkaHeaderCodec.apply(headers, record);
        Map<String, String> restored = KafkaHeaderCodec.toMap(record.headers());

        assertThat(restored)
                .containsEntry("x-snowdrift-trace-id", "trace-1")
                .containsEntry("x-snowdrift-username", "张三");
    }

    @Test
    @DisplayName("apply：null 或空 headers 不写入任何记录头")
    void applyNullOrEmptyIsNoOp() {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>("topic", new byte[]{1});

        KafkaHeaderCodec.apply(null, record);
        KafkaHeaderCodec.apply(Map.of(), record);

        assertThat(record.headers().toArray()).isEmpty();
    }

    @Test
    @DisplayName("apply：跳过 null key；null value 写为空字节")
    void applyHandlesNullKeyAndValue() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(null, "ignored");
        headers.put("k", null);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>("topic", new byte[]{1});

        KafkaHeaderCodec.apply(headers, record);

        Header[] all = record.headers().toArray();
        assertThat(all).hasSize(1);
        assertThat(all[0].key()).isEqualTo("k");
        assertThat(all[0].value()).isEmpty();
    }

    @Test
    @DisplayName("toMap：null Headers 返回空 Map")
    void toMapNullReturnsEmpty() {
        assertThat(KafkaHeaderCodec.toMap(null)).isEmpty();
    }

    @Test
    @DisplayName("toMap：跳过 null key 与 null value 的头")
    void toMapSkipsNullKeyAndValue() {
        Headers headers = new RecordHeaders();
        headers.add(new RecordHeader("good", "v".getBytes(StandardCharsets.UTF_8)));
        // Kafka 的 RecordHeader 构造器禁止 null key，这里用 mock 覆盖防御性分支
        Header nullKeyHeader = mock(Header.class);
        when(nullKeyHeader.key()).thenReturn(null);
        headers.add(nullKeyHeader);
        headers.add(new RecordHeader("nullValue", null));

        Map<String, String> result = KafkaHeaderCodec.toMap(headers);

        assertThat(result).containsExactly(Map.entry("good", "v"));
    }
}
