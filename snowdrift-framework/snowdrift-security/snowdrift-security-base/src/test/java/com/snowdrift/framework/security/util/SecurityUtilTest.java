package com.snowdrift.framework.security.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 令牌脱敏黑盒测试：空白/短令牌整体掩码、长令牌保留前后各 5 位、绝不泄露完整令牌，
 * 覆盖 12 位边界、连字符/下划线令牌、纯符号/中文令牌以及点分隔组合令牌的片段泄露边界
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

    @Test
    @DisplayName("边界：12 位令牌中段（2 位）被 ***** 覆盖")
    void twelveCharTokenMaskedMiddle() {
        assertThat(SecurityUtil.desensitizeToken("abcdefghijkl")).isEqualTo("abcde*****hijkl");
    }

    @Test
    @DisplayName("连字符与下划线属于令牌字符，同样保留前后各 5 位")
    void dashAndUnderscoreAreTokenChars() {
        assertThat(SecurityUtil.desensitizeToken("abcde-12345")).isEqualTo("abcde*****12345");
        assertThat(SecurityUtil.desensitizeToken("abcde_12345")).isEqualTo("abcde*****12345");
    }

    @Test
    @DisplayName("边界：整串都是非令牌字符（无连续 11 位可掩码）时整体掩码")
    void symbolsOnlyTokenOverallMasked() {
        assertThat(SecurityUtil.desensitizeToken("@#$%^&*()!@#$")).isEqualTo(MASK);
    }

    @Test
    @DisplayName("边界：中文等非 ASCII 字符不落入 [\\w-]，整体掩码避免原样打印")
    void cjkTokenOverallMasked() {
        assertThat(SecurityUtil.desensitizeToken("令牌令牌令牌令牌令牌令牌")).isEqualTo(MASK);
    }

    @Test
    @DisplayName("安全边界：点分隔的组合令牌中，长度不足 11 位的片段也不应原样输出")
    void shortSegmentInCompoundTokenNotPrintedVerbatim() {
        // 单看 "aaaaaaaaaa" 会整体掩码，但拼进含更长片段的令牌后若被原样打印，即为脱敏缺口
        String masked = SecurityUtil.desensitizeToken("aaaaaaaaaa.bbbbbbbbbbbb");

        assertThat(masked).doesNotContain("aaaaaaaaaa");
    }
}
