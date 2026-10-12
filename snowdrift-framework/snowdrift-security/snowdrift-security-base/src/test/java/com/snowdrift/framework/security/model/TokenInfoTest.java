package com.snowdrift.framework.security.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenInfoTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 统一 Token 响应模型黑盒测试：@Builder 全字段构造、未设值时的默认（字符串 null、
 * expiresIn 原始类型 0）、-1 永不过期约定、@Data 相等语义与可序列化契约
 * @since 1.0.0
 */
class TokenInfoTest {

    @Test
    @DisplayName("Builder：tokenValue/tokenName/prefix/expiresIn 均被如实写入")
    void builderSetsAllFields() {
        TokenInfo info = TokenInfo.builder()
                .tokenValue("abc123")
                .tokenName("Authorization")
                .prefix("Bearer")
                .expiresIn(3600L)
                .build();

        assertThat(info.getTokenValue()).isEqualTo("abc123");
        assertThat(info.getTokenName()).isEqualTo("Authorization");
        assertThat(info.getPrefix()).isEqualTo("Bearer");
        assertThat(info.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("默认值：字符串字段为 null，expiresIn 为原始类型 0（非 -1，即立即过期而非永不过期）")
    void builderDefaults() {
        TokenInfo info = TokenInfo.builder().build();

        assertThat(info.getTokenValue()).isNull();
        assertThat(info.getTokenName()).isNull();
        assertThat(info.getPrefix()).isNull();
        assertThat(info.getExpiresIn()).isZero();
    }

    @Test
    @DisplayName("边界：expiresIn=-1 表示永不过期，Builder 原样保留且 setter 可覆写")
    void minusOneMeansNeverExpire() {
        TokenInfo fromBuilder = TokenInfo.builder().expiresIn(-1L).build();
        assertThat(fromBuilder.getExpiresIn()).isEqualTo(-1L);

        fromBuilder.setExpiresIn(0L);
        assertThat(fromBuilder.getExpiresIn()).isZero();
    }

    @Test
    @DisplayName("@Data 契约：同值相等且 hashCode 一致，expiresIn 不同则不相等")
    void equalsAndHashCode() {
        TokenInfo a = TokenInfo.builder().tokenValue("t").tokenName("Authorization").expiresIn(60L).build();
        TokenInfo same = TokenInfo.builder().tokenValue("t").tokenName("Authorization").expiresIn(60L).build();
        TokenInfo diff = TokenInfo.builder().tokenValue("t").tokenName("Authorization").expiresIn(61L).build();

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(a).isNotEqualTo(diff);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a TokenInfo");
    }

    @Test
    @DisplayName("Serializable 契约：Java 序列化往返后字段保持一致")
    void serializationRoundTrip() {
        TokenInfo original = TokenInfo.builder()
                .tokenValue("tok")
                .tokenName("Authorization")
                .prefix("Bearer")
                .expiresIn(-1L)
                .build();

        TokenInfo restored = roundTrip(original);

        assertThat(restored).isNotSameAs(original).isEqualTo(original);
        assertThat(restored.getExpiresIn()).isEqualTo(-1L);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                out.writeObject(value);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Serializable 往返失败", e);
        }
    }
}
