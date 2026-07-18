package com.snowdrift.framework.cache.handler;

import com.snowdrift.framework.common.constant.StrConst;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 统一缓存 Key 生成器
 * <p>
 * 生成格式：{@code [prefix:]ClassName#methodName[:param1:param2...]}
 * </p>
 * <ul>
 *   <li>无参数时：{@code UserService#findById}</li>
 *   <li>简单参数（String / Number / Boolean / Enum）：直接使用 toString()</li>
 *   <li>复杂对象参数：使用 SHA-256 摘要前 16 位，避免 key 过长</li>
 *   <li>全局前缀（keyPrefix）非空时自动拼接到最前面</li>
 * </ul>
 *
 * <pre>
 * // 示例（prefix = "app"）
 * UserService#findById:123          → app:UserService#findById:123
 * OrderService#create:1,"VIP"       → app:OrderService#create:1:VIP
 * OrderService#batch:[a3f8c1d2e5...] → app:OrderService#batch:a3f8c1d2e5b74f09
 * </pre>
 *
 * @author gaoyzelov
 * @date 2026/6/13
 * @since 1.0.0
 */
public class SnowdriftKeyGenerator implements KeyGenerator {

    /**
     * 单个参数 toString 的最大长度，超过后使用摘要
     */
    private static final int MAX_PARAM_LENGTH = 64;

    /**
     * key 全局前缀，可为空
     */
    private final String keyPrefix;

    public SnowdriftKeyGenerator(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder sb = new StringBuilder();

        // 1. 前缀
        if (StringUtils.isNotBlank(keyPrefix)) {
            sb.append(keyPrefix).append(StrConst.COLON);
        }

        // 2. 类名#方法名
        sb.append(method.getDeclaringClass().getSimpleName())
          .append(StrConst.HASH)
          .append(method.getName());

        // 3. 参数
        if (params.length > 0) {
            sb.append(StrConst.COLON);
            StringJoiner joiner = new StringJoiner(StrConst.COLON);
            for (Object param : params) {
                joiner.add(toParamString(param));
            }
            sb.append(joiner);
        }

        return sb.toString();
    }

    /**
     * 将参数转为缓存 key 片段
     * <p>
     * 简单类型直接使用 toString()，复杂类型或超长字符串使用摘要。
     * </p>
     */
    private String toParamString(Object param) {
        if (Objects.isNull(param)) {
            return StrConst.EMPTY;
        }
        if (isSimpleType(param)) {
            String str = param.toString();
            return str.length() <= MAX_PARAM_LENGTH ? str : shortDigest(str);
        }
        // 复杂对象使用摘要
        return shortDigest(param);
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

    /**
     * 对对象生成 16 位十六进制摘要
     */
    private String shortDigest(Object obj) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(obj.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(32);
            for (int i = 0; i < 16 && i < bytes.length; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 在所有 JVM 中均可用，不会发生
            return Integer.toHexString(obj.hashCode());
        }
    }
}
