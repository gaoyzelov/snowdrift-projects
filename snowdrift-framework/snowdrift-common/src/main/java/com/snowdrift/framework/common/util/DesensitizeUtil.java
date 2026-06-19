package com.snowdrift.framework.common.util;

import com.snowdrift.framework.common.constant.StrConst;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * DesensitizeUtil
 *
 * @author 83674
 * @date 2026/3/31-17:28
 * @description 脱敏工具类
 * @since 1.0.0
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /**
     * 脱敏
     *
     * @param text    待脱敏文本
     * @param regex   正则表达式
     * @param replace 替换字符
     * @return 脱敏后的文本
     */
    public static String process(String text, String regex, String replace) {
        if (StringUtils.isBlank(text)) {
            return StrConst.EMPTY;
        }
        if (StringUtils.isBlank(regex)){
            return text;
        }
        return RegExUtils.replaceAll(text, regex, replace);
    }

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
        return RegExUtils.replaceAll(password, "(\\S+)", "**********");
    }

    /**
     * IP地址脱敏
     *
     * @param ip ip地址
     * @return 脱敏后的ip地址
     */
    public static String ip(String ip) {
        if (!IpUtil.isValidIp(ip)){
            return ip;
        }
        if (IpUtil.isIpv4(ip)) {
            return StringUtils.substringBefore(ip, StrConst.DOT) + ".*.*.*";
        }
        return StringUtils.substringBefore(ip, StrConst.COLON) + ":*:*:*:*:*:*:*";
    }

    /**
     * 手机号脱敏
     *
     * @param mobilePhone 手机号
     * @return 脱敏后的手机号
     */
    public static String mobilePhone(String mobilePhone) {
        if (StringUtils.isBlank(mobilePhone)) {
            return StrConst.EMPTY;
        }
        if (!ValidateUtil.isMobilePhone(mobilePhone)){
            return mobilePhone;
        }
        return RegExUtils.replaceAll(mobilePhone, "(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 固定电话脱敏
     *
     * @param fixedPhone 固定电话
     * @return 脱敏后的固定电话
     */
    public static String fixedPhone(String fixedPhone) {
        if (StringUtils.isBlank(fixedPhone)) {
            return StrConst.EMPTY;
        }
        if (!ValidateUtil.isFixedPhone(fixedPhone)){
            return fixedPhone;
        }
        return RegExUtils.replaceAll(fixedPhone, "(\\d+-)\\d+(\\d{4})", "$1****$2");
    }

    /**
     * 身份证号脱敏
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String idCard(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return StrConst.EMPTY;
        }
        if (!ValidateUtil.isIdCard(idCard)){
            return idCard;
        }
        return RegExUtils.replaceAll(idCard, "(\\d{4})\\d+(\\w{4})", "$1**********$2");
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
        if (!ValidateUtil.isEmail(email)) {
            return email;
        }
        return RegExUtils.replaceAll(email, "(^.)[^@]*(@.*$)", "$1****$2");
    }

    /**
     * 地址脱敏
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String address(String address) {
        if (StringUtils.isBlank(address)) {
            return StrConst.EMPTY;
        }
        int keepLen = Math.min(address.length(), 6);
        if (keepLen <= 3) {
            // 地址过短，全部掩码
            return "********";
        }
        return address.substring(0, keepLen) + "********";
    }

    /**
     * 中文姓名脱敏
     *
     * @param chineseName 中文姓名
     * @return 脱敏后的中文姓名
     */
    public static String chineseName(String chineseName) {
        if (StringUtils.isBlank(chineseName)) {
            return StrConst.EMPTY;
        }
        return chineseName.charAt(0) + "*".repeat(chineseName.length() - 1);
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
        if (!ValidateUtil.isBankCard(bankCard)) {
            return bankCard;
        }
        return RegExUtils.replaceAll(bankCard, "(\\d{4})\\d+(\\d{4})", "$1********$2");
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
        if (!ValidateUtil.isCarLicense(carLicense)){
            return carLicense;
        }
        return RegExUtils.replaceAll(carLicense, "([\\u4e00-\\u9fa5][A-Z])\\w+(\\w{1})", "$1****$2");
    }
}
