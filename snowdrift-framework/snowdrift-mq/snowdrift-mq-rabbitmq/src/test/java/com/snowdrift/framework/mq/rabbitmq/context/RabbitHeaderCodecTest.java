package com.snowdrift.framework.mq.rabbitmq.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RabbitHeaderCodecTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RabbitMQ MessageProperties 头转 broker 无关 Map 单元测试：字符串保留、非字符串 String 化与 null 安全
 * @since 1.0.0
 */
class RabbitHeaderCodecTest {

    @Test
    @DisplayName("null MessageProperties 返回空 Map")
    void nullPropertiesReturnsEmpty() {
        assertThat(RabbitHeaderCodec.toMap(null)).isEmpty();
    }

    @Test
    @DisplayName("无头时返回空 Map")
    void emptyHeadersReturnsEmpty() {
        assertThat(RabbitHeaderCodec.toMap(new MessageProperties())).isEmpty();
    }

    @Test
    @DisplayName("字符串头原样保留")
    void keepsStringHeaders() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-snowdrift-trace-id", "trace-1");
        properties.setHeader("x-snowdrift-username", "tom");

        Map<String, String> result = RabbitHeaderCodec.toMap(properties);

        assertThat(result)
                .containsEntry("x-snowdrift-trace-id", "trace-1")
                .containsEntry("x-snowdrift-username", "tom");
    }

    @Test
    @DisplayName("非字符串值统一 String 化（如 x-delay 数值头）")
    void stringifiesNonStringValues() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-delay", 5000L);
        properties.setHeader("flag", true);

        Map<String, String> result = RabbitHeaderCodec.toMap(properties);

        assertThat(result)
                .containsEntry("x-delay", "5000")
                .containsEntry("flag", "true");
    }

    @Test
    @DisplayName("跳过 null key 与 null value 的头")
    void skipsNullKeyAndValue() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("good", "v");
        properties.getHeaders().put(null, "ignored");
        properties.getHeaders().put("nullValue", null);

        Map<String, String> result = RabbitHeaderCodec.toMap(properties);

        assertThat(result).containsExactly(Map.entry("good", "v"));
    }
}
