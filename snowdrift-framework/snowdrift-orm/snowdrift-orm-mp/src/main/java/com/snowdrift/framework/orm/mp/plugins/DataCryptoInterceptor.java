package com.snowdrift.framework.orm.mp.plugins;

import com.snowdrift.framework.common.util.EncryptUtil;
import com.snowdrift.framework.common.util.ReflectUtil;
import com.snowdrift.framework.orm.core.anno.Encrypted;
import com.snowdrift.framework.orm.mp.properties.OrmMpCryptoProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据加解密 MyBatis 拦截器
 * <p>
 * 基于 MyBatis {@link Interceptor} 机制，在 SQL 执行前后对标注 {@link com.snowdrift.framework.orm.core.anno.Encrypted @Encrypted} 的字段进行自动加解密：
 * </p>
 * <ol>
 *   <li><b>写入时加密</b>：拦截 {@code Executor.update} 方法（INSERT/UPDATE/DELETE），
 *       遍历参数对象中所有 {@code @Encrypted} 字段，调用 {@link EncryptUtil#aesEncrypt} 加密后写入密文</li>
 *   <li><b>读取时解密</b>：拦截 {@code Executor.query} 方法（SELECT），
 *       遍历返回结果中所有 {@code @Encrypted} 字段，调用 {@link EncryptUtil#aesDecrypt} 解密密文</li>
 * </ol>
 * <p>
 * 密文格式：<code>{ENC} + Base64(AES密文)</code>，通过 {@code {ENC}} 前缀避免重复加密。<br>
 * 支持嵌套对象、Collection、Map 等复杂返回类型的递归处理。<br>
 * 通过 {@code snowdrift.orm.mp.crypto.enabled=true} 开启。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:11
 * @since 1.0.0
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
@AllArgsConstructor
public class DataCryptoInterceptor implements Interceptor {

    private static final String ENCRYPT_FLAG = "{ENC}";
    private final OrmMpCryptoProperties cryptoProperties;

    /**
     * MyBatis 拦截入口
     * <p>
     * 通过 {@code args.length} 区分操作类型：2 个参数为 update（INSERT/UPDATE/DELETE），4/6 个参数为 query（SELECT）。
     * </p>
     *
     * @param invocation MyBatis 调用上下文
     * @return SQL 执行结果（原样返回）
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        boolean isUpdate = args.length == 2;
        // 写入前加密参数（INSERT / UPDATE / DELETE）
        if (isUpdate) {
            Object param = args[1];
            if (param != null) {
                doIntercept(param, true);
            }
        }
        // 执行原始 SQL
        Object result = invocation.proceed();

        // 读取后解密结果（SELECT）
        if (!isUpdate && result != null) {
            doIntercept(result, false);
        }
        return result;
    }

    /**
     * 递归拦截处理：根据对象类型分发到对应的处理分支
     *
     * @param o       待处理对象（可能为实体 / Collection / Map）
     * @param encrypt {@code true} 加密，{@code false} 解密
     */
    private void doIntercept(Object o, boolean encrypt) {
        if (o == null || isBasicType(o.getClass())) {
            return;
        }
        if (o instanceof Collection<?> collection) {
            for (Object item : collection) {
                doCrypto(item, encrypt);
            }
        } else if (o instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                doCrypto(value, encrypt);
            }
        } else {
            doCrypto(o, encrypt);
        }
    }

    /**
     * 对单个对象的 {@code @Encrypted} 字段执行加解密
     * <p>
     * 遍历对象所有字段（含父类），找到标注 {@link Encrypted} 的 String 类型字段并处理。
     * 已加密的值不会重复加密，未加密的值不会解密。
     * </p>
     *
     * @param o       目标对象
     * @param encrypt {@code true} 加密，{@code false} 解密
     */
    private void doCrypto(Object o, boolean encrypt) {
        List<Field> fields = ReflectUtil.getDeclaredFields(o, true);
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(o);
                if (Objects.isNull(value)) {
                    continue;
                }
                if (field.isAnnotationPresent(Encrypted.class) && value instanceof String str) {
                    if (encrypt && StringUtils.isNotBlank(str) && !isEncrypted(str)) {
                        field.set(o, encryptValue(str));
                    }
                    if (!encrypt && StringUtils.isNotBlank(str) && isEncrypted(str)) {
                        field.set(o, decryptValue(str));
                    }
                }
            } catch (Exception e) {
                log.error("对象字段{}异常: {}.{}", encrypt ? "加密" : "解密", o.getClass().getSimpleName(), field.getName(), e);
            }
        }
    }

    /**
     * 判断字符串是否已被加密（以 {@value #ENCRYPT_FLAG} 开头）
     */
    private boolean isEncrypted(String text) {
        return text.startsWith(ENCRYPT_FLAG);
    }

    /**
     * AES 加密并添加密文前缀
     *
     * @param text 明文
     * @return {@code {ENC} + Base64(AES密文)}；密钥为空或已加密则返回原文
     */
    private String encryptValue(String text) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(cryptoProperties.getAesKey()) || text.startsWith(ENCRYPT_FLAG)) {
            return text;
        }
        return ENCRYPT_FLAG + EncryptUtil.aesEncrypt(text, cryptoProperties.getAesKey());
    }

    /**
     * AES 解密：移除密文前缀后解密
     *
     * @param text 密文（以 {@value #ENCRYPT_FLAG} 开头）
     * @return 明文；密钥为空或非密文格式则返回原文
     */
    private String decryptValue(String text) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(cryptoProperties.getAesKey()) || !text.startsWith(ENCRYPT_FLAG)) {
            return text;
        }
        return EncryptUtil.aesDecrypt(text.substring(ENCRYPT_FLAG.length()), cryptoProperties.getAesKey());
    }

    /**
     * 判断是否为基本类型（无需加解密处理）
     * <p>基本类型、String、Number、Boolean、Character 直接跳过。</p>
     *
     * @param clazz 待检测类型
     * @return {@code true} 为基础类型
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz == Character.class;
    }
}