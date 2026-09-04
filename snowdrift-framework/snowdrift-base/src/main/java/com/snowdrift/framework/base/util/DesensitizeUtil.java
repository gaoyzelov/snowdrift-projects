package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.constant.StrConst;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * DesensitizeUtil
 *
 * @author gaoyzelov
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
        // 归一化空白，避免号码内部空格干扰识别与掩码
        String compact = fixedPhone.replaceAll("\\s", StrConst.EMPTY);
        if (!ValidateUtil.isFixedPhone(compact)) {
            // 无法识别为固定电话时保守全掩码，避免原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        // 拆分区号与号码段（兼容 "010-66668888" / "66668888" 两种写法）
        String area = StrConst.EMPTY;
        String number = compact;
        int dashIndex = compact.indexOf(StrConst.MIDLINE);
        if (dashIndex > 0) {
            area = compact.substring(0, dashIndex);
            number = compact.substring(dashIndex + 1);
        }
        // 号码段仅保留后 4 位，其余掩码
        int tailLen = 4;
        String maskedNumber = number.length() <= tailLen
                ? StrConst.MASK_REPLACEMENT
                : "*".repeat(number.length() - tailLen) + number.substring(number.length() - tailLen);
        return area.isEmpty() ? maskedNumber : area + StrConst.MIDLINE + maskedNumber;
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
        return chineseName.charAt(0) + "*".repeat(Math.max(1, chineseName.length() - 1));
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
        // 归一化空白（如 "6222 8888 ..." 的分组写法），确保识别与掩码作用于连续数字
        String compact = bankCard.replaceAll("\\s", StrConst.EMPTY);
        if (!ValidateUtil.isBankCard(compact)) {
            // 无法识别为合法卡号时保守全掩码，避免原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        // 保留前 4 后 4，中间全部掩码（不改变号码长度）
        int headLen = 4;
        int tailLen = 4;
        int len = compact.length();
        return compact.substring(0, headLen)
                + "*".repeat(len - headLen - tailLen)
                + compact.substring(len - tailLen);
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
