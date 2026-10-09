package com.snowdrift.framework.base.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValidateUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 校验工具类单元测试
 * @since 1.0.0
 */
class ValidateUtilTest {

    /**
     * 按 GB 11643 的公开算法独立计算 18 位身份证校验码，用于构造真实可用的测试样本，
     * 避免直接复用被测实现（那样只能验证自洽，验证不了正确性）。
     */
    private static String checkDigitOf(String first17) {
        int[] weight = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] table = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (first17.charAt(i) - '0') * weight[i];
        }
        return String.valueOf(table[sum % 11]);
    }

    @Nested
    @DisplayName("手机号")
    class MobilePhone {

        @ParameterizedTest
        @ValueSource(strings = {"13812345678", "19912345678", "15000000000", "17012345678"})
        @DisplayName("合法：1 开头、次位 3-9、共 11 位")
        void valid(String phone) {
            assertThat(ValidateUtil.isMobilePhone(phone)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "12812345678",  // 次位 2，不在号段范围
                "1381234567",   // 10 位
                "138123456789", // 12 位
                "+8613812345678",
                "138 1234 5678",
                "1381234567a",
                "13812345678."})
        @DisplayName("非法：长度/次位/含非数字字符")
        void invalid(String phone) {
            assertThat(ValidateUtil.isMobilePhone(phone)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值与非空白校验：一律 false")
        void blank(String phone) {
            assertThat(ValidateUtil.isMobilePhone(phone)).isFalse();
        }
    }

    @Nested
    @DisplayName("固定电话")
    class FixedPhone {

        @ParameterizedTest
        @ValueSource(strings = {"010-66668888", "0755-12345678", "88888888", "010-1234567", "12345678"})
        @DisplayName("合法：可选 3~4 位区号 + 7~8 位号码")
        void valid(String phone) {
            assertThat(ValidateUtil.isFixedPhone(phone)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456", "010-123456789", "01066668888", "010-6666888a", "010--66668888"})
        @DisplayName("非法：位数不足或超长、缺分隔符")
        void invalid(String phone) {
            assertThat(ValidateUtil.isFixedPhone(phone)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void blank(String phone) {
            assertThat(ValidateUtil.isFixedPhone(phone)).isFalse();
        }
    }

    @Nested
    @DisplayName("身份证")
    class IdCard {

        @Test
        @DisplayName("18 位：校验码、大小写 X、地区码、出生日期均正确")
        void valid18() {
            assertThat(ValidateUtil.isIdCard("11010519491231002X")).isTrue();
            assertThat(ValidateUtil.isIdCard("11010519491231002x")).isTrue();

            // 用公开算法自行构造：110105（朝阳区）+ 20000229（闰日）+ 顺序码 002
            String first17 = "11010520000229002";
            assertThat(first17).hasSize(17);
            String leap = first17 + checkDigitOf(first17);
            assertThat(leap).hasSize(18);
            assertThat(ValidateUtil.isIdCard(leap)).isTrue();
        }

        @Test
        @DisplayName("18 位：校验码错误时拒绝")
        void wrongCheckDigit() {
            String wrong = "110105194912310021";
            assertThat(checkDigitOf("11010519491231002")).isEqualTo("X");
            assertThat(ValidateUtil.isIdCard(wrong)).isFalse();
        }

        @Test
        @DisplayName("15 位：合法样本通过，字母/长度不符拒绝")
        void valid15() {
            assertThat(ValidateUtil.isIdCard("110101900101123")).isTrue();
            assertThat(ValidateUtil.isIdCard("11010190010112A")).isFalse();
            assertThat(ValidateUtil.isIdCard("11010190010112")).isFalse();
        }

        @Test
        @DisplayName("15 位：非 ASCII 数字不通过（Character::isDigit 会放行 Unicode 数字）")
        void fifteenDigitRequiresAsciiDigits() {
            String ascii = "110101900101123";
            // 末三位换成阿拉伯-印度数字 ١٢٣，其余为 ASCII 数字
            String unicodeDigits = "110101900101" + "١٢٣";

            assertThat(unicodeDigits).hasSize(15);
            assertThat(unicodeDigits).isNotEqualTo(ascii);
            assertThat(ValidateUtil.isIdCard(ascii)).isTrue();
            assertThat(ValidateUtil.isIdCard(unicodeDigits)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "11010519491331002X",   // 月份 13
                "11010519490230002X",   // 2 月 30 日
                "11010519490229002X",   // 1949 不是闰年
                "99010519491231002X",   // 地区码 99 不在省级前缀表内
                "11010520991231002X",   // 出生年份晚于当前年份
                "11010518991231002X",   // 出生年份早于 1900
                "11010519491231002",    // 17 位
                "11010519491231002XX"}) // 19 位
        @DisplayName("非法：日期、地区码、未来年份、长度")
        void invalid(String idCard) {
            assertThat(ValidateUtil.isIdCard(idCard)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "1101051949123100 X"})
        void blank(String idCard) {
            assertThat(ValidateUtil.isIdCard(idCard)).isFalse();
        }
    }

    @Nested
    @DisplayName("银行卡")
    class BankCard {

        @ParameterizedTest
        @ValueSource(strings = {"6222888888888888", "6222 8888 8888 8888", "6222888888888888888", "6222 8888 8888 8888 888"})
        @DisplayName("合法：16/19 位，允许 4 位分组空格")
        void valid(String card) {
            assertThat(ValidateUtil.isBankCard(card)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"622288888888888", "62228888888888888888", "6222-8888-8888-8888", "622288888888888a"})
        @DisplayName("非法：长度不符、分隔符不合法")
        void invalid(String card) {
            assertThat(ValidateUtil.isBankCard(card)).isFalse();
        }

        @Test
        @DisplayName("宽松点：每个 4 位分组前的空格是独立可选的，不要求整串分隔风格统一")
        void groupSeparatorIsOptionalPerGroup() {
            assertThat(ValidateUtil.isBankCard("62228888 88888888")).isTrue();
            assertThat(ValidateUtil.isBankCard("6222 88888888 8888")).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void blank(String card) {
            assertThat(ValidateUtil.isBankCard(card)).isFalse();
        }
    }

    @Nested
    @DisplayName("车架号 / 车牌 / 邮箱 / URL")
    class Others {

        @Test
        void vin() {
            assertThat(ValidateUtil.isVIN("LSVNL49J232029620")).isTrue();
            assertThat(ValidateUtil.isVIN("LSVNL49J23202962I")).isFalse(); // I 不在允许字符集
            assertThat(ValidateUtil.isVIN("LSVNL49J23202962O")).isFalse(); // O 不在允许字符集
            assertThat(ValidateUtil.isVIN("LSVNL49J2320296200")).isFalse();
            assertThat(ValidateUtil.isVIN("lsvnl49j232029620")).isFalse();
            assertThat(ValidateUtil.isVIN(null)).isFalse();
        }

        @Test
        void carLicense() {
            assertThat(ValidateUtil.isCarLicense("京A12345")).isTrue();
            assertThat(ValidateUtil.isCarLicense("京AD12345")).isTrue();
            assertThat(ValidateUtil.isCarLicense("京A1234")).isFalse();
            assertThat(ValidateUtil.isCarLicense("A12345")).isFalse();
            assertThat(ValidateUtil.isCarLicense("12345")).isFalse();
            assertThat(ValidateUtil.isCarLicense(null)).isFalse();
        }

        @Test
        @DisplayName("车牌宽松点：省份后的字母位只要求 [A-Z]，未排除实际不使用的 I/O")
        void carLicenseTolerance() {
            assertThat(ValidateUtil.isCarLicense("京I12345")).isTrue();
            assertThat(ValidateUtil.isCarLicense("京O12345")).isTrue();
        }

        @Test
        void email() {
            assertThat(ValidateUtil.isEmail("test@example.com")).isTrue();
            assertThat(ValidateUtil.isEmail("a.b+c%_d@sub.example.co")).isTrue();
            assertThat(ValidateUtil.isEmail("test@example")).isFalse();
            assertThat(ValidateUtil.isEmail("@example.com")).isFalse();
            assertThat(ValidateUtil.isEmail("test@.com")).isFalse();
            assertThat(ValidateUtil.isEmail("test@example.c")).isFalse();
            assertThat(ValidateUtil.isEmail(null)).isFalse();
        }

        @Test
        @DisplayName("URL 仅做形状校验（主机名结构不在校验范围内）")
        void url() {
            assertThat(ValidateUtil.isURL("https://example.com")).isTrue();
            assertThat(ValidateUtil.isURL("http://example.com/a?b=1#c")).isTrue();
            assertThat(ValidateUtil.isURL("ftp://example.com")).isTrue();
            assertThat(ValidateUtil.isURL("example.com")).isFalse();
            assertThat(ValidateUtil.isURL("https://")).isFalse();
            assertThat(ValidateUtil.isURL("   ")).isFalse();
            assertThat(ValidateUtil.isURL(null)).isFalse();
        }
    }
}
