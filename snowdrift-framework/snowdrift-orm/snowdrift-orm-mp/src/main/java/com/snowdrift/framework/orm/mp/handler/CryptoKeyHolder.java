package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.common.exception.BizException;
import org.apache.commons.lang3.StringUtils;

/**
 * AES 加密密钥静态持有器
 * <p>
 * TypeHandler 由 MyBatis 实例化，无法通过 Spring 注入配置属性。
 * 该类提供静态方法存取 AES 密钥，由 Spring 配置类在启动时通过
 * {@link com.snowdrift.framework.orm.mp.properties.OrmMpBaseProperties#getCryptoKey()} 初始化。
 * </p>
 * <p>
 * 线程安全：使用 {@code volatile} 保证密钥的可见性，写入发生在 Spring 容器启动阶段，
 * 后续运行时仅读取，不存在竞态问题。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/14
 * @since 1.0.0
 */
public final class CryptoKeyHolder {

    private static volatile String cryptoKey;

    private CryptoKeyHolder() {
    }

    /**
     * 设置 AES 密钥（十六进制字符串）
     * <p>
     * 由 Spring 配置类在启动时调用，运行时不应重复调用。
     * </p>
     *
     * @param key AES 密钥（十六进制字符串，16/24/32 字节对应 AES-128/192/256）
     */
    public static void setKey(String key) {
        cryptoKey = key;
    }

    /**
     * 获取 AES 密钥
     *
     * @return AES 密钥（十六进制字符串）
     * @throws BizException 密钥未初始化时抛出
     */
    public static String getKey() {
        if (StringUtils.isBlank(cryptoKey)) {
            throw new BizException("orm.crypto.key.not.configured");
        }
        return cryptoKey;
    }

    /**
     * 判断密钥是否已配置
     *
     * @return true 密钥已设置且非空
     */
    public static boolean isKeyAvailable() {
        return StringUtils.isNotBlank(cryptoKey);
    }
}
