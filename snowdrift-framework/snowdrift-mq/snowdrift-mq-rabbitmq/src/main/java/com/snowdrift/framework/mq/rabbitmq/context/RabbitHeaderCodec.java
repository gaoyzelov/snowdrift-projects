package com.snowdrift.framework.mq.rabbitmq.context;

import org.springframework.amqp.core.MessageProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RabbitMQ {@link MessageProperties} 头与 broker 无关 {@code Map<String,String>} 互转
 * <p>消费端从 {@code MessageProperties.getHeaders()} 读头供上下文恢复；非字符串值统一 String 化。</p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
public final class RabbitHeaderCodec {

    private RabbitHeaderCodec() {
    }

    /**
     * 把 AMQP 消息属性头读取为 broker 无关 Map
     */
    public static Map<String, String> toMap(MessageProperties messageProperties) {
        Map<String, String> result = new LinkedHashMap<>();
        if (messageProperties == null || messageProperties.getHeaders() == null) {
            return result;
        }
        messageProperties.getHeaders().forEach((key, value) -> {
            if (key != null && value != null) {
                result.put(key, value.toString());
            }
        });
        return result;
    }
}
