package com.snowdrift.framework.cache.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 统一缓存 Key 生成器
 * <p>
 * 生成格式：{@code 全限定类名#方法名[:参数类型:参数值...]}（每个参数占「类型、值」两段）
 * </p>
 * <ul>
 *   <li>无参数时：{@code com.example.UserService#findById}</li>
 *   <li>简单参数（{@link CharSequence} / {@link Number} / {@link Boolean} / {@link Enum} / {@link Character}）：
 *       直接使用 {@code toString()}；{@code null} 参数写出空片段</li>
 *   <li>复杂对象参数：使用<b>字段序列化</b>（fastjson2 {@code FieldBased}）而非 {@code toString()}，
 *       以保证跨实例、跨 JVM 稳定；片段超过 64 字符时取<b>完整 SHA-256 十六进制摘要（64 字符）</b>，
 *       并加 {@code sha256:} 前缀以区别于「本身就是该摘要串」的参数</li>
 *   <li>参数类型必须参与 key：同名重载方法（{@code m(Integer)} / {@code m(Long)} / {@code m(String)}）
 *       若只写参数值，值相同时会撞成同一个 key</li>
 *   <li>每个参数片段中的 {@code \} 与 {@code :} 会被转义，保证 {@code m("a:b")} 与 {@code m("a", "b")} 不撞 key</li>
 *   <li><b>本类不拼接 {@code snowdrift.cache.key-prefix}</b>：前缀由各后端加，Redis 侧由
 *       {@code RedisCacheConfiguration#computePrefixWith} 拼成 {@code prefix:cacheName:key}，
 *       Caffeine 侧不加前缀。{@code ICacheService} 的 key 前缀则在 {@code AbstractCacheService#buildKey} 中拼接</li>
 * </ul>
 *
 * <pre>
 * // 示例（方法签名 OrderService#create(Long id, String level)）
 * com.example.UserService#findById:java.lang.Long:123
 * com.example.OrderService#create:java.lang.Long:1:java.lang.String:VIP
 * com.example.OrderService#batch:java.lang.String:sha256:9f2a...（超长参数取 SHA-256 摘要）
 * </pre>
 * <p>
 * <b>已知歧义：</b>{@code null} 参数与空字符串参数都会生成空片段，因此 {@code m(null)} 与 {@code m("")}
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
     * 单个参数片段的最大长度，超过后使用摘要
     */
    private static final int MAX_PARAM_LENGTH = 64;

    /**
     * 超长参数摘要的前缀
     * <p>加前缀是为了与「恰好等于某个摘要十六进制串」的原始参数区分开：
     * 摘要固定 64 个十六进制字符，与阈值相同，不加前缀时两者会拼出同一个 key。</p>
     */
    private static final String DIGEST_PREFIX = "sha256:";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder sb = new StringBuilder();
        // 1. 完整类名#方法名：用 simpleName 会让不同包下的同名类（p1.Inner / p2.Inner）撞同一个 key
        sb.append(method.getDeclaringClass().getName())
          .append(StrConst.HASH)
          .append(method.getName());

        // 2. 参数：类型 + 值。类型必须参与，否则同名重载方法（m(Integer)/m(Long)/m(String)）
        //    在参数值相同时会生成同一个 key，同一 cache 名下会互相读到错误数据
        if (params.length > 0) {
            StringJoiner joiner = new StringJoiner(StrConst.COLON);
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                Class<?> type = i < parameterTypes.length
                        ? parameterTypes[i]
                        : (params[i] == null ? Object.class : params[i].getClass());
                joiner.add(escapeDelimiter(type.getName()));
                joiner.add(escapeDelimiter(toParamString(params[i])));
            }
            sb.append(StrConst.COLON).append(joiner);
        }

        return sb.toString();
    }

    /**
     * 将参数转为缓存 key 片段
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
        String str = isSimpleType(param) ? param.toString() : stableSerialize(param);
        return str.length() <= MAX_PARAM_LENGTH ? str : DIGEST_PREFIX + EncryptUtil.sha256(str);
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
     * 转义参数片段中的分隔符
     * <p>
     * 若不转义，{@code m("a:b")} 与 {@code m("a", "b")} 会拼出同一个 key，
     * 从而命中彼此的缓存并返回错误数据。反斜杠须先转义，保证结果可逆。
     * </p>
     */
    private String escapeDelimiter(String fragment) {
        return fragment.replace("\\", "\\\\").replace(StrConst.COLON, "\\:");
    }

    /**
     * 判断是否为简单类型（可直接 toString 作为 key 片段）
     */
    private boolean isSimpleType(Object param) {
        return param instanceof CharSequence
                || param instanceof Number
                || param instanceof Boolean
                || param instanceof Enum
                || param instanceof Character;
    }
}
