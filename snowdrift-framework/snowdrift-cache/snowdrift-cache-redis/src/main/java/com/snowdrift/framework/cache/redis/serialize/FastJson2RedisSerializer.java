package com.snowdrift.framework.cache.redis.serialize;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * 基于 FastJSON2 的 Redis 序列化器（供 Spring Cache @Cacheable 使用）
 * <p><b>信任前提：</b>反序列化启用了 {@code SupportAutoType} 以还原写入的 {@code @type}，
 * 因此仅应读取由本应用自身写入的缓存值；若缓存存在被外部篡改风险，请勿使用本序列化器。</p>
 *
 * @author gaoye
 * @since 1.0.0
 */
public class FastJson2RedisSerializer implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        return JSON.toJSONString(value, JSONWriter.Feature.WriteClassName, JSONWriter.Feature.FieldBased)
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return JSON.parseObject(new String(bytes, StandardCharsets.UTF_8), Object.class,
                JSONReader.Feature.SupportAutoType,
                JSONReader.Feature.FieldBased);
    }
}
