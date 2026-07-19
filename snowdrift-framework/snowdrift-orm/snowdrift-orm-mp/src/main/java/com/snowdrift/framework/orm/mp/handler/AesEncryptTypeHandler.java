package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.EncryptUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AES 加密字段处理器
 * <p>
 * 使用 AES/GCM 认证加密（推荐），兼容旧 ECB 格式数据的解密。
 * </p>
 * <ul>
 *   <li>新数据：{@code {AES_GCM}<Base64(iv[12B] + ciphertext + tag[16B])>}</li>
 *   <li>旧数据：{@code {ENC}<Base64(ecb_ciphertext)>}（仅解密，写入时自动升级为 GCM）</li>
 * </ul>
 *
 * @author gaoyzelov
 * @date 2026/7/14-10:51
 * @description 加密字段处理
 * @since 1.0.0
 */
public class AesEncryptTypeHandler implements TypeHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptTypeHandler.class);

    /** 新格式前缀：AES/GCM */
    private static final String GCM_FLAG = "{ENC2}";
    /** 旧格式前缀：AES/ECB（兼容解密） */
    private static final String ECB_FLAG = "{ENC}";

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        String encryptedValue = doEncrypt(parameter);
        ps.setString(i, encryptedValue);
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        String encryptedValue = rs.getString(columnName);
        return doDecrypt(encryptedValue);
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        String encryptedValue = rs.getString(columnIndex);
        return doDecrypt(encryptedValue);
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encryptedValue = cs.getString(columnIndex);
        return doDecrypt(encryptedValue);
    }

    /**
     * 加密 —— 使用 AES/GCM
     */
    private String doEncrypt(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        if (text.startsWith(GCM_FLAG)) {
            throw new BizException("orm.crypto.reject.enc.prefix");
        }
        if (text.startsWith(ECB_FLAG)) {
            throw new BizException("orm.crypto.reject.enc.prefix");
        }
        if (!CryptoKeyHolder.isKeyAvailable()) {
            throw new BizException("orm.crypto.key.unavailable");
        }
        return GCM_FLAG + EncryptUtil.aesGcmEncrypt(text, CryptoKeyHolder.getKey());
    }

    /**
     * 解密 —— 兼容 GCM（新）和 ECB（旧）
     */
    private String doDecrypt(String encryptedValue) {
        if (StringUtils.isBlank(encryptedValue)) {
            return encryptedValue;
        }
        if (!CryptoKeyHolder.isKeyAvailable()) {
            throw new BizException("orm.crypto.key.unavailable");
        }
        // GCM 格式（新）
        if (encryptedValue.startsWith(GCM_FLAG)) {
            return EncryptUtil.aesGcmDecrypt(
                    encryptedValue.substring(GCM_FLAG.length()), CryptoKeyHolder.getKey());
        }
        // ECB 格式（旧，兼容解密）
        if (encryptedValue.startsWith(ECB_FLAG)) {
            log.debug("解密旧 ECB 格式加密字段，数据将在下次写入时升级为 GCM");
            return EncryptUtil.aesEcbDecrypt(
                    encryptedValue.substring(ECB_FLAG.length()), CryptoKeyHolder.getKey());
        }
        // 明文
        return encryptedValue;
    }
}
