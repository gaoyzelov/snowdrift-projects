package com.snowdrift.framework.mq.convert;

import com.alibaba.fastjson2.JSON;

/**
 * FastJson2 消息转换器（默认实现）
 * <p>
 * 基于 FastJson2 进行消息序列化/反序列化，保证 String / byte[] / POJO 三类 payload 往返对称：
 * <ul>
 *   <li>{@code String}：写端 JSON 编码（含引号），读端 {@code JSON.parseObject} 解码还原；</li>
 *   <li>{@code byte[]}：写端原样透传，读端原样直取（不做 JSON 数组编码）；</li>
 *   <li>其它 POJO：JSON 编码 / 解码。</li>
 * </ul>
 * 如需切换序列化方案（如 Jackson），注册一个 {@link MqMessageConverter} 类型的 Bean 即可覆盖。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
public class FastJson2MqMessageConverter implements MqMessageConverter {

    @Override
    public byte[] serialize(Object payload) {
        if (payload == null) {
            return JSON.toJSONBytes(null);
        }
        if (payload instanceof byte[]) {
            // 原始字节透传，避免 JSON 数组编码造成往返不对称
            return (byte[]) payload;
        }
        return JSON.toJSONBytes(payload);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> targetType) {
        if (byte[].class.equals(targetType)) {
            return targetType.cast(data);
        }
        if (String.class.equals(targetType)) {
            // serialize 端 String 走 JSON 编码（含引号），此处需 JSON 解码还原原始字符串
            Object parsed = JSON.parseObject(data, String.class);
            return targetType.cast(parsed);
        }
        return JSON.parseObject(data, targetType);
    }
}
