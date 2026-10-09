package com.snowdrift.framework.cache.serialize;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

import java.lang.reflect.Type;

/**
 * 基于 Fastjson2 的缓存序列化器
 * <p>
 * 安全设计：
 * <ul>
 *   <li>不写入 {@code @type} 类型元数据（关闭 {@link JSONWriter.Feature#WriteClassName}）</li>
 *   <li>反序列化依赖调用方传入的 {@link Class} 参数，而非 JSON 内嵌类型</li>
 *   <li>忽略未知字段，兼容不同版本间的数据结构差异</li>
 * </ul>
 * </p>
 * <p>
 * <b>读写必须同为字段模式（{@code FieldBased}）：</b>写侧用 {@link JSONWriter.Feature#FieldBased} 把私有字段写进
 * JSON（不要求 getter），读侧若不加 {@link JSONReader.Feature#FieldBased}，fastjson2 只认 setter/公开字段——
 * 对于「私有字段 + getter、无 setter」的类，JSON 里明明有值却读不回来，且<b>不报错</b>，
 * 调用方会拿到一个字段全是默认值的对象（静默数据丢失）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/19
 * @since 1.0.0
 */
public class FastJson2CacheSerializer extends AbstractCacheSerializer {

    @Override
    protected String doSerialize(Object value) {
        // FieldBased：只按字段读写（与 JacksonCacheSerializer 的字段口径一致）
        // SortMapEntriesByKeys：Map 键按字母序输出，与 Jackson 的 ORDER_MAP_ENTRIES_BY_KEYS 对齐；
        //   同时让 HashMap 之类的无序容器产出确定性的 JSON，便于逐字比对与排查
        return JSON.toJSONString(value, JSONWriter.Feature.FieldBased, JSONWriter.Feature.SortMapEntriesByKeys);
    }

    @Override
    protected <T> T doDeserialize(String json, Class<T> type) {
        return JSON.parseObject(json, type, JSONReader.Feature.FieldBased);
    }

    @Override
    protected <T> T doDeserialize(String json, Type type) {
        return JSON.parseObject(json, type, JSONReader.Feature.FieldBased);
    }
}
