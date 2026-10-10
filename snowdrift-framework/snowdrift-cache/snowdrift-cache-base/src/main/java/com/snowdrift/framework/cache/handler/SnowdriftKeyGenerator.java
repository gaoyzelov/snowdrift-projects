package com.snowdrift.framework.cache.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 统一缓存 Key 生成器
 * <p>
 * 生成格式：{@code 简单类名#方法名[:参数段摘要]}；无参数时不带摘要，带参数时把各参数的
 * 「类型:值」按序拼接（值转义）成参数段，整体取 SHA-256 前 128 位摘要（32 个十六进制字符），
 * key 长度恒定、参数值不进入明文 key。
 * </p>
 * <ul>
 *   <li>无参数时：{@code UserService#findById}（不摘要）</li>
 *   <li>摘要输入口径——简单参数（{@link CharSequence} / {@link Number} / {@link Boolean} / {@link Enum} /
 *       {@link Character}）：直接使用 {@code toString()}；{@code null} 参数写出空片段</li>
 *   <li>摘要输入口径——复杂对象参数：使用<b>字段序列化</b>（fastjson2 {@code FieldBased}）而非 {@code toString()}，
 *       保证跨实例、跨 JVM 稳定；序列化失败回退 {@code toString()} 并告警</li>
 *   <li>参数类型必须参与摘要输入：同名重载方法（{@code m(Integer)} / {@code m(Long)} / {@code m(String)}）
 *       若只写参数值，值相同时会摘要出同一个 key；类型用简单名以控制输入长度</li>
 *   <li>参数段中的 {@code \} 与 {@code :} 转义后再摘要，保证 {@code m("a:String:b")} 与 {@code m("a", "b")}
 *       不会拼出相同的摘要输入</li>
 *   <li>摘要为 <b>SHA-256 前 128 位</b>（32 个十六进制字符，统一摘要后参数段只有这一种形态，无需前缀）：无实用
 *       碰撞攻击（生日碰撞界 2^64），且输出长度恒定，参数再长 key 也不变长</li>
 *   <li><b>类名与类型名均使用简单名</b>（{@link Class#getSimpleName()}）以缩短 key；代价是不同包下的
 *       同名类若共用同一 cacheName 且方法名、参数相同会撞 key，属已知取舍</li>
 *   <li><b>本类不拼接 {@code snowdrift.cache.key-prefix}</b>：前缀由各后端加，Redis 侧由
 *       {@code RedisCacheConfiguration#computePrefixWith} 拼成 {@code prefix:cacheName:key}，
 *       Caffeine 侧不加前缀。{@code ICacheService} 的 key 前缀则在 {@code AbstractCacheService#buildKey} 中拼接</li>
 * </ul>
 *
 * <pre>
 * // 示例（方法签名 OrderService#create(Long id, String level)）
 * UserService#findById（无参数：不摘要）
 * OrderService#create:8b1d4a...（参数段整体摘要：SHA-256 前 128 位，32 个十六进制字符）
 * </pre>
 * <p>
 * <b>已知歧义：</b>{@code null} 参数与空字符串参数会写出相同的摘要输入，因此 {@code m(null)} 与 {@code m("")}
 * 的 key 相同。若业务需要区分二者，请勿让它们共用同一个缓存方法。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/13
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftKeyGenerator implements KeyGenerator {

    /**
     * 摘要保留的十六进制长度（SHA-256 前 128 位）
     * <p>缓存 key 场景 128 位的生日碰撞界（2^64）已足够；截短使参数段固定 32 字符、key 长度恒定。</p>
     */
    private static final int DIGEST_HEX_LENGTH = 32;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder(64);
        // 简单类名#方法名：完整类名会让 key 明显过长，简单名的撞 key 边界见类注释
        key.append(typeName(method.getDeclaringClass()))
           .append(StrConst.HASH)
           .append(method.getName());
        if (params.length == 0) {
            return key.toString();
        }

        // 摘要输入：类型 + 值。类型必须参与，否则同名重载方法（m(Integer)/m(Long)/m(String)）
        // 在参数值相同时会摘要出同一个 key，同一 cache 名下会互相读到错误数据
        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder section = new StringBuilder(64);
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                section.append(StrConst.COLON);
            }
            section.append(typeName(parameterTypes[i]))
                   .append(StrConst.COLON)
                   .append(escapeDelimiter(toParamString(params[i])));
        }
        return key.append(StrConst.COLON).append(digest(section.toString())).toString();
    }

    /**
     * 将参数转为摘要输入片段（未转义）
     * <p>
     * 简单类型直接使用 {@code toString()}；复杂对象使用字段序列化，避免
     * {@code ClassName@identityHashCode} 形式的 {@code toString()} 使 key 跨实例/跨 JVM 不稳定
     * （在 Redis 后端下表现为永远不命中）。
     * </p>
     */
    private String toParamString(Object param) {
        if (Objects.isNull(param)) {
            return StrConst.EMPTY;
        }
        return isSimpleType(param) ? param.toString() : stableSerialize(param);
    }

    /**
     * 复杂对象的稳定序列化
     * <p>
     * 使用字段序列化（{@code FieldBased}）：POJO 即使没有 getter 也能反映字段值，
     * 避免序列化成空对象后不同实例撞成同一个 key。序列化失败时回退 {@code toString()} 并告警
     * —— 此时 key 可能跨实例不稳定，属已知退化路径。
     * </p>
     */
    private String stableSerialize(Object param) {
        try {
            return JSON.toJSONString(param, JSONWriter.Feature.FieldBased);
        } catch (Exception e) {
            log.warn("缓存 key 参数序列化失败，回退 toString()（key 可能跨实例不稳定）: class={}, err={}",
                    param.getClass().getName(), e.getMessage());
            return String.valueOf(param);
        }
    }

    /**
     * 类/类型的 key 展示名
     * <p>
     * 使用 {@link Class#getSimpleName()} 控制 key 长度；匿名类等简单名为空的场景回退全限定名
     * （全限定名不含 {@code :} 与 {@code \}，无需转义）。
     * </p>
     */
    private String typeName(Class<?> type) {
        String simpleName = type.getSimpleName();
        return simpleName.isEmpty() ? type.getName() : simpleName;
    }

    /**
     * 参数段整体摘要
     * <p>
     * 取 SHA-256 前 128 位（32 个十六进制字符）：无实用碰撞攻击（生日碰撞界 2^64），
     * 输出长度恒定，参数值再长 key 也不增长。
     * </p>
     */
    private String digest(String section) {
        return EncryptUtil.sha256(section).substring(0, DIGEST_HEX_LENGTH);
    }

    /**
     * 转义参数片段中的分隔符
     * <p>
     * 若不转义，{@code m("a:String:b")} 与 {@code m("a", "b")} 会拼出相同的摘要输入，
     * 从而摘要出同一个 key 并返回错误数据。反斜杠须先转义，保证结果可逆。
     * </p>
     */
    private String escapeDelimiter(String fragment) {
        return fragment.replace("\\", "\\\\").replace(StrConst.COLON, "\\:");
    }

    /**
     * 判断是否为简单类型（可直接 toString 作为摘要输入片段）
     */
    private boolean isSimpleType(Object param) {
        return param instanceof CharSequence
                || param instanceof Number
                || param instanceof Boolean
                || param instanceof Enum
                || param instanceof Character;
    }
}
