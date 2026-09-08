package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.EncryptUtil;
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
 * 使用 AES/GCM 认证加密，存储格式为 {@code {ENC}<Base64(iv[12B] + ciphertext + tag[16B])>}，
 * 由 {@link EncryptUtil#aesGcmEncrypt(String, String)} 生成，仅支持 GCM 这一种格式。
 * </p>
 * <ul>
 *   <li>带 {@code {ENC}} 前缀的密文：解密时走 AES/GCM 解密；</li>
 *   <li>不带前缀的值：按明文原样返回（不做解密）。</li>
 * </ul>
 * <p><b>加密列使用限制</b>：TypeHandler 仅在 ORM 参数绑定与结果集映射时生效，SQL 中的
 * WHERE / ORDER BY / 唯一性约束等片段不会经过本处理器，因此对加密列做等值、排序或去重比较时，
 * 参与比较的仍是密文、结果不可用。加密列的等值查询需在 Wrapper 层先将明文加密后再比较
 * （{@link com.snowdrift.framework.orm.mp.util.WrapperUtil} 目前为占位实现，尚未提供该能力），
 * 设计表结构与查询时应避免将加密列用于过滤、排序或唯一键。</p>
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
            throw new BizException("不能加密已加密的数据");
        }
        if (!CryptoKeyHolder.isKeyAvailable()) {
            throw new BizException("数据加密密钥不可用");
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
            throw new BizException("数据加密密钥不可用");
        }
        // GCM 格式
        if (encryptedValue.startsWith(ENC_FLAG)) {
            return EncryptUtil.aesGcmDecrypt(encryptedValue.substring(ENC_FLAG.length()), CryptoKeyHolder.getKey());
        }
        // 明文
        return encryptedValue;
    }
}
