package com.snowdrift.framework.security.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 令牌脱敏黑盒测试：空白/短令牌整体掩码、长令牌保留前后各 5 位、绝不泄露完整令牌
 * @since 1.0.0
 */
class SecurityUtilTest {

    private static final String MASK = "******";

    @Test
    @DisplayName("null/空/空白令牌整体掩码")
    void blankTokenMasked() {
        assertThat(SecurityUtil.desensitizeToken(null)).isEqualTo(MASK);
        assertThat(SecurityUtil.desensitizeToken("")).isEqualTo(MASK);
        assertThat(SecurityUtil.desensitizeToken("   ")).isEqualTo(MASK);
    }

    @Test
    @DisplayName("边界：长度不足以同时保留前后各 5 位（<=10）时整体掩码")
    void shortTokenMasked() {
        assertThat(SecurityUtil.desensitizeToken("abc")).isEqualTo(MASK);
        assertThat(SecurityUtil.desensitizeToken("abcdefghij")).isEqualTo(MASK); // 10 位
    }

    @Test
    @DisplayName("边界：恰好 11 位时保留前 5 后 5，中间以 ***** 覆盖")
    void exactlyElevenCharsMaskedMiddle() {
        assertThat(SecurityUtil.desensitizeToken("abcdefghijk")).isEqualTo("abcde*****ghijk");
    }

    @Test
    @DisplayName("UUID 令牌：保留前后各 5 位，掩码中段，且不等于原值")
    void uuidTokenMasked() {
        String token = "550e8400-e29b-41d4-a716-446655440000";

        String masked = SecurityUtil.desensitizeToken(token);

        assertThat(masked).startsWith("550e8").endsWith("40000").contains("*****");
        assertThat(masked).isNotEqualTo(token);
        // 安全断言：脱敏结果不得包含原令牌的中间敏感段
        assertThat(masked).doesNotContain("e29b-41d4");
    }

    @Test
    @DisplayName("无连字符的 32 位十六进制令牌同样正确脱敏")
    void hexTokenMasked() {
        String token = "0123456789abcdef0123456789abcdef";

        String masked = SecurityUtil.desensitizeToken(token);

        assertThat(masked).isEqualTo("01234*****bcdef");
    }
}
