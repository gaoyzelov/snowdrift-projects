package com.snowdrift.core.utils;

import com.snowdrift.core.constant.StrConst;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * DesensitizeUtil
 *
 * @author gaoye
 * @date 2025/03/20 14:33:00
 * @description 脱敏工具类
 * @since 1.0.0
 */
public class DesensitizeUtil {

    /**
     * 密码脱敏
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String password(String password) {
        if (StringUtils.isBlank(password)) {
            return StrConst.EMPTY;
        }
        return StringUtils.repeat(StrConst.ASTERISK, password.length());
    }

    /**
     * 邮箱脱敏
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String email(String email) {
        if (StringUtils.isBlank(email)) {
            return StrConst.EMPTY;
        }
        int pos = StringUtils.indexOf(email, StrConst.AT);
        if (pos <= 1) {
            return email;
        }
        return StringUtils.repeat(StrConst.ASTERISK, pos) + StringUtils.substring(email, pos);
    }

    /**
     * 地址脱敏
     *
     * @param address 地址
     * @param size    脱敏长度
     * @return 脱敏后的地址
     */
    public static String address(String address, int size) {
        if (StringUtils.isBlank(address)) {
            return StrConst.EMPTY;
        }
        int length = address.length();
        if (length <= size) {
            return StringUtils.repeat(StrConst.ASTERISK, length);
        }
        return StringUtils.substring(address, 0, length - size) + StringUtils.repeat(StrConst.ASTERISK, size);
    }

    /**
     * 姓名脱敏
     *
     * @param chineseName 姓名
     * @return 脱敏后的姓名
     */
    public static String chineseName(String chineseName) {
        if (StringUtils.isBlank(chineseName)) {
            return StrConst.EMPTY;
        }
        int length = chineseName.length();
        return StringUtils.substring(chineseName, 0, 1) + StringUtils.repeat(StrConst.ASTERISK, length - 1);
    }

    /**
     * 证件号脱敏
     *
     * @param idCard 证件号
     * @return 脱敏后的证件号
     */
    public static String idCard(String idCard) {
        return mask(idCard, 6, 4);
    }

    /**
     * 银行卡号脱敏
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String bankCard(String bankCard) {
        if (StringUtils.isBlank(bankCard)) {
            return StrConst.EMPTY;
        }
        bankCard = RegExUtils.replaceAll(bankCard, StrConst.SPACE, StrConst.EMPTY);
        if (bankCard.length() < 9) {
            return bankCard;
        }
        int length = bankCard.length();
        int endLength = length % 4 == 0 ? 4 : length % 4;
        int midLength = length - 4 - endLength;
        int midCount = midLength / 4;
        String repeat = StringUtils.repeat(StrConst.ASTERISK, 4);
        StringBuilder sb = new StringBuilder();
        sb.append(bankCard, 0, 4);
        for (int i = 0; i < midCount; i++) {
            sb.append(StrConst.SPACE).append(repeat);
        }
        sb.append(StrConst.SPACE).append(bankCard, length - endLength, length);
        return sb.toString();
    }

    /**
     * 手机号脱敏
     *
     * @param mobilePhone 手机号
     * @return 脱敏后的手机号
     */
    public static String mobilePhone(String mobilePhone) {
        return mask(mobilePhone, 3, 4);
    }

    /**
     * 座机号脱敏，只保留前4位和后2位，中间全部替换为 *
     *
     * @param fixedPhone 座机号
     * @return 脱敏后的座机号
     */
    public static String fixedPhone(String fixedPhone) {
        return mask(fixedPhone, 4, 2);
    }

    /**
     * IPV4地址脱敏
     *
     * @param ipv4 IPV4地址
     * @return 脱敏后的IPV4地址
     */
    public static String ipv4(String ipv4) {
        if (StringUtils.isBlank(ipv4)) {
            return StrConst.EMPTY;
        }
        int pos = ipv4.indexOf(StrConst.DOT);
        if (pos <= 0) {
            return ipv4;
        }
        return StringUtils.substring(ipv4, 0, pos) + ".*.*.*";
    }

    /**
     * IPV6地址脱敏
     *
     * @param ipv6 IPV6地址
     * @return 脱敏后的IPV6地址
     */
    public static String ipv6(String ipv6) {
        if (StringUtils.isBlank(ipv6)) {
            return StrConst.EMPTY;
        }
        int pos = ipv6.indexOf(StrConst.COLON);
        if (pos <= 0) {
            return ipv6;
        }
        return StringUtils.substring(ipv6, 0, pos) + ":*:*:*:*:*:*:*";
    }

    /**
     * 车牌号脱敏
     *
     * @param carLicense 车牌号
     * @return 脱敏后的车牌号
     */
    public static String carLicense(String carLicense) {
        if (StringUtils.isBlank(carLicense)) {
            return StrConst.EMPTY;
        }
        if (carLicense.length() != 7 && carLicense.length() != 8) {
            return carLicense;
        }
        return mask(carLicense, 3, 1);
    }

    /**
     * 字符串脱敏
     *
     * @param str   字符串
     * @param front 前缀保留位
     * @param back  后缀保留位
     * @return 脱敏后的字符串
     */
    private static String mask(String str, int front, int back) {
        if (StringUtils.isBlank(str)) {
            return StrConst.EMPTY;
        }
        int length = str.length();
        if (length < (front + back)) {
            return StrConst.EMPTY;
        }
        if (front < 0 || back < 0) {
            return StrConst.EMPTY;
        }
        return StringUtils.substring(str, 0, front) + StringUtils.repeat(StrConst.ASTERISK, length - front - back) + StringUtils.substring(str, length - back);
    }
}