package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.EncryptUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 加密字段处理
 * @author gaoyzelov
 * @date 2026/7/14-10:51
 * @description 加密字段处理
 * @since 1.0.0
 */
public class AesEncryptTypeHandler implements TypeHandler<String> {

    private static final String ENCRYPT_FLAG = "{ENC}";

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
     * 加密
     * @param text 明文
     * @return 密文
     */
    private String doEncrypt(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        if (text.startsWith(ENCRYPT_FLAG)) {
            throw new BizException("orm.crypto.reject.enc.prefix");
        }
        if (!CryptoKeyHolder.isKeyAvailable()) {
            throw new BizException("orm.crypto.key.unavailable");
        }
        return ENCRYPT_FLAG + EncryptUtil.aesEncrypt(text, CryptoKeyHolder.getKey());
    }

    /**
     * 解密-如果密钥不可用则返回密文
     * @param encryptedValue 密文
     * @return 明文
     */
    private String doDecrypt(String encryptedValue) {
        if (StringUtils.isBlank(encryptedValue) || !encryptedValue.startsWith(ENCRYPT_FLAG)) {
            return encryptedValue;
        }
        if (!CryptoKeyHolder.isKeyAvailable()){
            throw new BizException("orm.crypto.key.unavailable");
        }
        return EncryptUtil.aesDecrypt(encryptedValue.substring(ENCRYPT_FLAG.length()), CryptoKeyHolder.getKey());
    }
}
