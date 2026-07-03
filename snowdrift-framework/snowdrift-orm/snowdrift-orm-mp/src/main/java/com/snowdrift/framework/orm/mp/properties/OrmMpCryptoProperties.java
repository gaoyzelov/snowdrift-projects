package com.snowdrift.framework.orm.mp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * OrmMpCryptoProperties
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:26
 * @description 数据加解密配置属性
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.orm.mp.crypto")
public class OrmMpCryptoProperties implements Serializable {

    /**
     * 是否启用字段加解密（默认关闭）
     */
    private Boolean enabled;

    /**
     * AES 加密密钥（16/24/32 字节对应 AES-128/192/256）
     */
    private String aesKey;
}
