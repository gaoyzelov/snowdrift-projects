package com.snowdrift.framework.mq.core;

import com.alibaba.fastjson2.JSON;

/**
 * FastJson2 消息转换器（默认实现）
 * <p>
 * 基于 FastJson2 进行消息序列化/反序列化。
 * 如需切换序列化方案（如 Jackson），注册一个 {@link MqMessageConverter} 类型的 Bean 即可覆盖。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
public class FastJson2MqMessageConverter implements MqMessageConverter {

    @Override
    public byte[] serialize(Object payload) {
        return JSON.toJSONBytes(payload);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> targetType) {
        if (byte[].class.equals(targetType)) {
            return targetType.cast(data);
        }
        if (String.class.equals(targetType)) {
            return targetType.cast(new String(data, java.nio.charset.StandardCharsets.UTF_8));
        }
        return JSON.parseObject(data, targetType);
    }
}
