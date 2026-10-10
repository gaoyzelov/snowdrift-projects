package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.orm.mp.CryptoKeyHolder;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AesEncryptTypeHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description AES 字段处理器黑盒测试：加解密往返、明文透传、重复加密拒绝、缺密钥 fail-closed 与前缀大小写不敏感
 * @since 1.0.0
 */
class AesEncryptTypeHandlerTest {

    /** 32 位十六进制 = 16 字节 = AES-128 */
    private static final String KEY = "0123456789abcdef0123456789abcdef";
    private static final String ENC_FLAG = "{ENC}";

    private final AesEncryptTypeHandler handler = new AesEncryptTypeHandler();

    @BeforeEach
    void setUp() {
        CryptoKeyHolder.setKey(KEY);
    }

    @AfterEach
    void reset() {
        CryptoKeyHolder.setKey(null);
    }

    private String encryptViaHandler(String plain) throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setParameter(ps, 1, plain, JdbcType.VARCHAR);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ps).setString(eq(1), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("写入加密→读取解密往返还原，密文带 {ENC} 前缀")
    void encryptDecryptRoundTrip() throws SQLException {
        String stored = encryptViaHandler("13800000000");

        assertThat(stored).startsWith(ENC_FLAG).isNotEqualTo("13800000000");

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn(stored);
        assertThat(handler.getResult(rs, "phone")).isEqualTo("13800000000");
    }

    @Test
    @DisplayName("每次加密生成随机 IV，相同明文两次密文不同，但都能解密还原")
    void encryptionIsNonDeterministic() throws SQLException {
        String c1 = encryptViaHandler("same");
        String c2 = encryptViaHandler("same");

        assertThat(c1).isNotEqualTo(c2);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(c1);
        when(rs.getString(2)).thenReturn(c2);
        assertThat(handler.getResult(rs, 1)).isEqualTo("same");
        assertThat(handler.getResult(rs, 2)).isEqualTo("same");
    }

    @Test
    @DisplayName("读取无前缀的值按明文原样返回（密钥关闭时也可读）")
    void plaintextPassesThrough() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("plain-text");

        assertThat(handler.getResult(rs, "col")).isEqualTo("plain-text");
    }

    @Test
    @DisplayName("边界：空白/null 值加解密均原样返回")
    void blankAndNullPassThrough() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setParameter(ps, 1, null, JdbcType.VARCHAR);
        handler.setParameter(ps, 2, "", JdbcType.VARCHAR);
        verify(ps).setString(eq(1), eq(null));
        verify(ps).setString(eq(2), eq(""));

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("c")).thenReturn("");
        assertThat(handler.getResult(rs, "c")).isEmpty();
    }

    @Test
    @DisplayName("安全：拒绝再次加密已带 {ENC} 前缀的数据（大小写不敏感）")
    void rejectsDoubleEncryption() {
        PreparedStatement ps = mock(PreparedStatement.class);

        assertThatThrownBy(() -> handler.setParameter(ps, 1, "{ENC}abc", JdbcType.VARCHAR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能加密已加密的数据");
        assertThatThrownBy(() -> handler.setParameter(ps, 1, "{enc}abc", JdbcType.VARCHAR))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("安全：加密时密钥不可用则 fail-closed 抛错")
    void encryptFailsWhenKeyUnavailable() {
        CryptoKeyHolder.setKey(null);
        PreparedStatement ps = mock(PreparedStatement.class);

        assertThatThrownBy(() -> handler.setParameter(ps, 1, "secret", JdbcType.VARCHAR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密钥不可用");
    }

    @Test
    @DisplayName("安全：读取密文但密钥不可用则抛错，绝不把密文当明文返回")
    void decryptFailsWhenKeyUnavailable() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("{ENC}whatever");
        CryptoKeyHolder.setKey(null);

        assertThatThrownBy(() -> handler.getResult(rs, "col"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密钥不可用");
    }

    @Test
    @DisplayName("读取 {enc} 小写前缀的密文仍能正确解密（前缀判断忽略大小写）")
    void decryptIsCaseInsensitiveOnPrefix() throws SQLException {
        String stored = encryptViaHandler("case-test");
        String lowerPrefixed = "{enc}" + stored.substring(ENC_FLAG.length());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn(lowerPrefixed);

        assertThat(handler.getResult(rs, "col")).isEqualTo("case-test");
    }

    @Test
    @DisplayName("CallableStatement 读取路径同样解密")
    void callableStatementDecrypts() throws SQLException {
        String stored = encryptViaHandler("cs-value");
        CallableStatement cs = mock(CallableStatement.class);
        when(cs.getString(1)).thenReturn(stored);

        assertThat(handler.getResult(cs, 1)).isEqualTo("cs-value");
    }
}
