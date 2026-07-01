package com.snowdrift.framework.mq.core;

/**
 * MQ 消息转换器 SPI
 * <p>
 * 负责消息体的序列化与反序列化。
 * 默认实现为 {@link com.snowdrift.framework.mq.core.FastJson2MqMessageConverter}（FastJson2），
 * 用户可通过注册 {@link MqMessageConverter} 类型的 Bean 覆盖，切换到 Jackson、Protobuf 等方案。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
public interface MqMessageConverter {

    /**
     * 序列化消息体为字节数组
     *
     * @param payload 消息体
     * @return 字节数组
     */
    byte[] serialize(Object payload);

    /**
     * 反序列化字节数组为指定类型
     *
     * @param data       字节数组
     * @param targetType 目标类型
     * @param <T>        泛型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] data, Class<T> targetType);
}
