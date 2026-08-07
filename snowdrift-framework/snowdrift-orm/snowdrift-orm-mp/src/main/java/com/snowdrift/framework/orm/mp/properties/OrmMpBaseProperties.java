package com.snowdrift.framework.orm.mp.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

/**
 * OrmMpBaseProperties
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:26
 * @description 数据加解密配置属性
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.orm.mp")
public class OrmMpBaseProperties implements Serializable {

    /**
     * 是否启用乐观锁（默认关闭）
     */
    @NotNull
    private Boolean optimisticLock = Boolean.FALSE;

    /**
     * 是否启用字段加解密（默认关闭）
     */
    @NotNull
    private Boolean crypto = Boolean.FALSE;

    /**
     * AES 加密密钥（十六进制字符串，16/24/32 字节对应 AES-128/192/256）
     */
    private String cryptoKey;
}
