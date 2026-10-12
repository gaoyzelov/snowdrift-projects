package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.constant.StrConst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DesensitizeUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 脱敏工具类单元测试
 * @since 1.0.0
 */
class DesensitizeUtilTest {

    /** 扩展 B 区汉字，UTF-16 下占两个 char（代理对） */
    private static final String SUPPLEMENTARY_CHAR = "𠀋";

    /** 判断字符串中是否残留孤立代理（脱敏截断代理对的直接表现） */
    private static boolean hasUnpairedSurrogate(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    return true;
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                return true;
            }
        }
        return false;
    }

    @Nested
    @DisplayName("手机号")
    class MobilePhone {

        @Test
        void normal() {
            assertThat(DesensitizeUtil.mobilePhone("13812345678")).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("归一化内部空白后再掩码")
        void withWhitespace() {
            assertThat(DesensitizeUtil.mobilePhone("138 1234 5678")).isEqualTo("138****5678");
            assertThat(DesensitizeUtil.mobilePhone("  13812345678  ")).isEqualTo("138****5678");
        }

        @ParameterizedTest
        @ValueSource(strings = {"garbage", "1381234567", "12812345678", "138-1234-5678"})
        @DisplayName("无法识别为手机号时保守全掩码，不原样输出")
        void unrecognizedIsFullyMasked(String phone) {
            assertThat(DesensitizeUtil.mobilePhone(phone)).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void blank(String phone) {
            assertThat(DesensitizeUtil.mobilePhone(phone)).isEmpty();
        }
    }

    @Nested
    @DisplayName("固定电话")
    class FixedPhone {

        @Test
        @DisplayName("保留区号，号码段仅留后 4 位")
        void normal() {
            assertThat(DesensitizeUtil.fixedPhone("010-66668888")).isEqualTo("010-****8888");
            assertThat(DesensitizeUtil.fixedPhone("0755-12345678")).isEqualTo("0755-****5678");
        }

        @Test
        @DisplayName("无区号时只输出掩码后的号码段")
        void withoutAreaCode() {
            assertThat(DesensitizeUtil.fixedPhone("66668888")).isEqualTo("****8888");
        }

        @ParameterizedTest
        @ValueSource(strings = {"garbage", "123456"})
        void unrecognizedIsFullyMasked(String phone) {
            assertThat(DesensitizeUtil.fixedPhone(phone)).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void blank(String phone) {
            assertThat(DesensitizeUtil.fixedPhone(phone)).isEmpty();
        }
    }

    @Nested
    @DisplayName("身份证")
    class IdCard {

        @Test
        @DisplayName("合法 18/15 位保留前 4 后 4，长度不变")
        void normal() {
            String id18 = DesensitizeUtil.idCard("11010519491231002X");
            assertThat(id18).isEqualTo("1101**********002X").hasSameSizeAs("11010519491231002X");

            String id15 = DesensitizeUtil.idCard("110101900101123");
            assertThat(id15).isEqualTo("1101*******1123").hasSameSizeAs("110101900101123");
        }

        @ParameterizedTest
        @ValueSource(strings = {"110105194912310021", "not-an-id", "11010519491231002"})
        @DisplayName("非法号码保守全掩码，不泄漏任何一位")
        void invalidIsFullyMasked(String idCard) {
            assertThat(DesensitizeUtil.idCard(idCard)).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void blank(String idCard) {
            assertThat(DesensitizeUtil.idCard(idCard)).isEmpty();
        }
    }

    @Nested
    @DisplayName("邮箱 / 银行卡 / 车牌 / 密码")
    class Others {

        @Test
        void email() {
            assertThat(DesensitizeUtil.email("test@example.com")).isEqualTo("t****@example.com");
            assertThat(DesensitizeUtil.email("a@b.cn")).isEqualTo("a****@b.cn");
        }

        @Test
        void bankCard() {
            assertThat(DesensitizeUtil.bankCard("6222 8888 8888 8888")).isEqualTo("6222********8888");
            assertThat(DesensitizeUtil.bankCard("6222888888888888")).isEqualTo("6222********8888");
        }

        @Test
        void carLicense() {
            assertThat(DesensitizeUtil.carLicense("京A12345")).isEqualTo("京A****5");
        }

        @Test
        void password() {
            assertThat(DesensitizeUtil.password("secret")).isEqualTo("**********");
        }

        @ParameterizedTest
        @ValueSource(strings = {"garbage", "not-an-email@", "6222888888888888x", "12345"})
        @DisplayName("各类非法输入一律全掩码")
        void invalidInputsAreFullyMasked(String text) {
            assertThat(DesensitizeUtil.email(text)).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @Test
        void blankInputs() {
            assertThat(DesensitizeUtil.email(null)).isEmpty();
            assertThat(DesensitizeUtil.bankCard("  ")).isEmpty();
            assertThat(DesensitizeUtil.carLicense(null)).isEmpty();
            assertThat(DesensitizeUtil.password("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("IP 地址")
    class Ip {

        @Test
        void ipv4AndIpv6() {
            assertThat(DesensitizeUtil.ip("192.168.1.1")).isEqualTo("192.*.*.*");
            assertThat(DesensitizeUtil.ip("10.0.0.1")).isEqualTo("10.*.*.*");
            assertThat(DesensitizeUtil.ip("2001:db8::1")).isEqualTo("2001:*:*:*:*:*:*:*");
        }

        @Test
        @DisplayName("带首尾空白的合法 IP 仍被识别并掩码（不再原样输出）")
        void trimsBeforeMatching() {
            assertThat(DesensitizeUtil.ip("192.168.1.100 ")).isEqualTo("192.*.*.*");
            assertThat(DesensitizeUtil.ip("\n2001:db8::1\t")).isEqualTo("2001:*:*:*:*:*:*:*");
        }

        @ParameterizedTest
        @ValueSource(strings = {"192.168.1.100:8080", "host.internal", "not-an-ip", "192.168.1", "999.999.999.999"})
        @DisplayName("非 IP 写法保守全掩码，避免主机名/端口等原样泄漏")
        void unrecognizedIsFullyMasked(String ip) {
            assertThat(DesensitizeUtil.ip(ip)).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @Test
        @DisplayName("压缩 IPv6 的保留前缀为空时输出去掉了整段地址（结果无信息，但同样不泄漏）")
        void compressedIpv6KeepsNothing() {
            // ::1 / ::%zzz 这类以冒号开头的写法，按第一个冒号切分得到空前缀
            assertThat(DesensitizeUtil.ip("::1")).isEqualTo(":*:*:*:*:*:*:*");
            assertThat(DesensitizeUtil.ip("::1")).doesNotContain("1");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void blank(String ip) {
            assertThat(DesensitizeUtil.ip(ip)).isEmpty();
        }
    }

    @Nested
    @DisplayName("地址")
    class Address {

        @Test
        @DisplayName("保留前 6 个码点，其余掩码")
        void normal() {
            assertThat(DesensitizeUtil.address("北京市朝阳区望京SOHO大厦")).isEqualTo("北京市朝阳区******");
            assertThat(DesensitizeUtil.address("北京市朝阳区望京")).isEqualTo("北京市朝阳区******");
        }

        @ParameterizedTest
        @ValueSource(strings = {"北京市朝阳区", "北京市朝阳", "abcdef", "abcd", "abc", "北京", "a"})
        @DisplayName("6 个码点以内整体掩码：保留前缀覆盖整串时掩码形同虚设")
        void shortAddressIsFullyMasked(String address) {
            assertThat(DesensitizeUtil.address(address)).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @Test
        @DisplayName("代理对不被从中间截断（原实现会抛越界或输出孤立代理）")
        void neverSplitsSurrogatePair() {
            String address = "abcde" + SUPPLEMENTARY_CHAR + "北京市朝阳区";
            String masked = DesensitizeUtil.address(address);

            assertThat(masked).isEqualTo("abcde" + SUPPLEMENTARY_CHAR + "******");
            assertThat(hasUnpairedSurrogate(masked)).isFalse();
        }

        @Test
        @DisplayName("首字符即为扩展区汉字时同样不截断")
        void supplementaryCharAtBoundary() {
            // 9 个码点：1 个扩展区汉字 + 8 个汉字，保留前 6 个码点（扩展区汉字 + 5 个汉字）
            String address = "𝕏" + "北京市朝阳区望京";
            assertThat(address.codePointCount(0, address.length())).isEqualTo(9);

            String masked = DesensitizeUtil.address(address);

            assertThat(masked).isEqualTo("𝕏" + "北京市朝阳" + "******");
            assertThat(hasUnpairedSurrogate(masked)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void blank(String address) {
            assertThat(DesensitizeUtil.address(address)).isEmpty();
        }
    }

    @Nested
    @DisplayName("中文姓名")
    class ChineseName {

        @Test
        void normal() {
            assertThat(DesensitizeUtil.chineseName("张三丰")).isEqualTo("张**");
            assertThat(DesensitizeUtil.chineseName("张三")).isEqualTo("张*");
        }

        @Test
        @DisplayName("单字姓名至少保留一个掩码字符")
        void singleCharName() {
            assertThat(DesensitizeUtil.chineseName("张")).isEqualTo("张*");
        }

        @Test
        @DisplayName("按码点保留首字，扩展区汉字不被切开")
        void neverSplitsSurrogatePair() {
            String name = SUPPLEMENTARY_CHAR + "三丰";
            String masked = DesensitizeUtil.chineseName(name);

            assertThat(masked).isEqualTo(SUPPLEMENTARY_CHAR + "**");
            assertThat(hasUnpairedSurrogate(masked)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void blank(String name) {
            assertThat(DesensitizeUtil.chineseName(name)).isEmpty();
        }
    }

    @Nested
    @DisplayName("process 底层正则替换")
    class Process {

        @Test
        void replacesMatches() {
            assertThat(DesensitizeUtil.process("13812345678", "(\\d{3})\\d{4}(\\d{4})", "$1****$2"))
                    .isEqualTo("138****5678");
        }

        @Test
        @DisplayName("空白文本返回空串；正则缺失时 fail-closed 整体掩码，绝不原样返回明文")
        void edgeInputs() {
            assertThat(DesensitizeUtil.process(null, "\\d", "*")).isEmpty();
            assertThat(DesensitizeUtil.process("  ", "\\d", "*")).isEmpty();
            // 正则缺失是调用方的编程错误，此时「原样返回」等于把明文当脱敏结果交出去。
            // 同类专用脱敏方法在参数非法时一律返回整体掩码，此处对齐该约定。
            assertThat(DesensitizeUtil.process("abc", "", "*")).isEqualTo(StrConst.MASK_REPLACEMENT);
            assertThat(DesensitizeUtil.process("abc", null, "*")).isEqualTo(StrConst.MASK_REPLACEMENT);
        }

        @Test
        @DisplayName("安全边界：正则缺失时不得泄露原文（如手机号、身份证）")
        void missingRegexNeverLeaksPlaintext() {
            assertThat(DesensitizeUtil.process("13812345678", "  ", "*"))
                    .doesNotContain("13812345678")
                    .isEqualTo(StrConst.MASK_REPLACEMENT);
            assertThat(DesensitizeUtil.process("110101199003074567", null, "*"))
                    .doesNotContain("110101199003074567");
        }

        @Test
        @DisplayName("未匹配时原样返回，说明它不能当作自动识别的脱敏入口")
        void unmatchedReturnsInputUnchanged() {
            assertThat(DesensitizeUtil.process("abc", "\\d+", "*")).isEqualTo("abc");
        }

        @Test
        @DisplayName("批量号码可逐个脱敏且长度不变")
        void keepsLengthForAllSamples() {
            assertThat(IntStream.range(0, 20)
                    .mapToObj(i -> "1381234" + String.format("%04d", i))
                    .map(DesensitizeUtil::mobilePhone))
                    .allSatisfy(masked -> assertThat(masked).hasSameSizeAs("13812345678"))
                    .allMatch(masked -> masked.startsWith("138****"));
        }
    }
}
