package com.snowdrift.framework.mq.convert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FastJson2MqMessageConverterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 默认 FastJson2 转换器单元测试：验证 String / byte[] / POJO 三类 payload 的往返对称性与边界处理
 * @since 1.0.0
 */
class FastJson2MqMessageConverterTest {

    private final FastJson2MqMessageConverter converter = new FastJson2MqMessageConverter();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderEvent {
        private String orderId;
        private long amount;
    }

    @Test
    @DisplayName("POJO 往返：序列化再反序列化还原等价对象")
    void roundTripPojo() {
        OrderEvent origin = new OrderEvent("order-1", 99L);

        byte[] bytes = converter.serialize(origin);
        OrderEvent restored = converter.deserialize(bytes, OrderEvent.class);

        assertThat(restored).usingRecursiveComparison().isEqualTo(origin);
    }

    @Test
    @DisplayName("String 往返：写端 JSON 编码含引号，读端解码还原原始字符串")
    void roundTripString() {
        byte[] bytes = converter.serialize("hello");

        // serialize 对 String 走 JSON 编码，字节应带引号
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("\"hello\"");
        assertThat(converter.deserialize(bytes, String.class)).isEqualTo("hello");
    }

    @Test
    @DisplayName("String 内容本身是 JSON 文本时仍对称还原，不被二次解析")
    void roundTripJsonLikeString() {
        String jsonLike = "{\"a\":1}";

        byte[] bytes = converter.serialize(jsonLike);

        assertThat(converter.deserialize(bytes, String.class)).isEqualTo(jsonLike);
    }

    @Test
    @DisplayName("byte[] 往返：原样透传，不做 JSON 数组编码")
    void roundTripBytes() {
        byte[] origin = {1, 2, 3, 4};

        byte[] serialized = converter.serialize(origin);

        // 原始字节透传：序列化结果与入参为同一份内容
        assertThat(serialized).containsExactly(origin);
        assertThat(converter.deserialize(serialized, byte[].class)).containsExactly(origin);
    }

    @Test
    @DisplayName("边界：serialize(null) 产出可反序列化回 null 的字节")
    void serializeNull() {
        byte[] bytes = converter.serialize(null);

        assertThat(bytes).isNotNull();
        assertThat(converter.deserialize(bytes, OrderEvent.class)).isNull();
    }

    @Test
    @DisplayName("边界：deserialize 目标为 byte[] 时直接返回原始数据（含 null）")
    void deserializeBytesTargetReturnsRawData() {
        byte[] data = {9, 8, 7};

        assertThat(converter.deserialize(data, byte[].class)).isSameAs(data);
        assertThat(converter.deserialize(null, byte[].class)).isNull();
    }
}
