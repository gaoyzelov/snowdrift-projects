package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.EncryptUtil;
import com.snowdrift.framework.orm.mp.CryptoKeyHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

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
 *   <li>新数据：{@code {ENC}<Base64(iv[12B] + ciphertext + tag[16B])>}</li>
 *   <li>旧数据：{@code {ENC}<Base64(ecb_ciphertext)>}（仅解密）</li>
 * </ul>
 *
 * @author gaoyzelov
 * @date 2026/7/14-10:51
 * @description 加密字段处理
 * @since 1.0.0
 */
public class AesEncryptTypeHandler implements TypeHandler<String> {

    /**
     * 加密前缀
     */
    private static final String ENC_FLAG = "{ENC}";

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
        if (StringUtils.startsWithIgnoreCase(text, ENC_FLAG)) {
            throw new BizException("orm.crypto.reject.enc.prefix");
        }
        if (!CryptoKeyHolder.isKeyAvailable()) {
            throw new BizException("orm.crypto.key.unavailable");
        }
        return ENC_FLAG + EncryptUtil.aesGcmEncrypt(text, CryptoKeyHolder.getKey());
    }

    /**
     * 解密 —— 使用 AES/GCM
     */
    private String doDecrypt(String encryptedValue) {
        if (StringUtils.isBlank(encryptedValue)) {
            return encryptedValue;
        }
        if (!CryptoKeyHolder.isKeyAvailable()) {
            throw new BizException("orm.crypto.key.unavailable");
        }
        // GCM 格式
        if (encryptedValue.startsWith(ENC_FLAG)) {
            return EncryptUtil.aesGcmDecrypt(encryptedValue.substring(ENC_FLAG.length()), CryptoKeyHolder.getKey());
        }
        // 明文
        return encryptedValue;
    }
}
