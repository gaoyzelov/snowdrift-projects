package com.snowdrift.framework.base.util;

import com.google.common.net.InetAddresses;
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
     * <p>
     * 正则缺失（null / 空白）属于调用方的编程错误，此时 <b>fail-closed</b> 返回整体掩码
     * {@link StrConst#MASK_REPLACEMENT}，而不是把原文原样交出去 —— 后者会让「脱敏」变成明文的
     * 直通通道（手机号、身份证等会以「已脱敏」的名义落进日志）。本类专用的脱敏方法在参数非法时
     * 同样返回整体掩码，此处与之一致。
     * </p>
     *
     * @param text    待脱敏文本
     * @param regex   正则表达式；缺失时整体掩码
     * @param replace 替换字符
     * @return 脱敏后的文本
     */
    public static String process(String text, String regex, String replace) {
        if (StringUtils.isBlank(text)) {
            return StrConst.EMPTY;
        }
        if (StringUtils.isBlank(regex)){
            return StrConst.MASK_REPLACEMENT;
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
        return process(password, "(\\S+)", "**********");
    }

    /**
     * IP地址脱敏
     *
     * @param ip ip地址
     * @return 脱敏后的ip地址
     */
    public static String ip(String ip) {
        if (StringUtils.isBlank(ip)) {
            return StrConst.EMPTY;
        }
        // 归一化空白后再识别：IP 常被拼入日志文本，尾部空格/换行不应导致漏脱敏
        String compact = StringUtils.trim(ip);
        if (!IpUtil.isValidIp(compact)) {
            // 无法识别为 IP 时保守全掩码，避免 hostname、host:port 等原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        if (IpUtil.isIpv4(compact)) {
            return StringUtils.substringBefore(compact, StrConst.DOT) + ".*.*.*";
        }
        return StringUtils.substringBefore(compact, StrConst.COLON) + ":*:*:*:*:*:*:*";
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
        // 归一化空白，避免号码内部空格干扰识别与掩码
        String compact = mobilePhone.replaceAll("\\s", StrConst.EMPTY);
        if (!ValidateUtil.isMobilePhone(compact)) {
            // 无法识别为手机号时保守全掩码，避免原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        return process(compact, "(\\d{3})\\d{4}(\\d{4})", "$1****$2");
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
        // 归一化空白，避免号码内部空格干扰识别与掩码
        String compact = idCard.replaceAll("\\s", StrConst.EMPTY);
        if (!ValidateUtil.isIdCard(compact)) {
            // 无法识别为身份证号时保守全掩码，避免原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        // 保留前 4 后 4，中间按实际长度掩码（15 位与 18 位输出长度均与输入一致）
        int headLen = 4;
        int tailLen = 4;
        int len = compact.length();
        if (len <= headLen + tailLen) {
            return StrConst.MASK_REPLACEMENT;
        }
        return compact.substring(0, headLen)
                + "*".repeat(len - headLen - tailLen)
                + compact.substring(len - tailLen);
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
        // 归一化空白（邮箱本地部分不允许空格，移除不影响合法邮箱），再识别
        String compact = email.replaceAll("\\s", StrConst.EMPTY);
        if (!ValidateUtil.isEmail(compact)) {
            // 无法识别为邮箱时保守全掩码，避免原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        return process(compact, "(^.)[^@]*(@.*$)", "$1****$2");
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
        // 先按码点计数再按码点推进：length() 是 UTF-16 单元数，直接用它截取会把补充平面字符
        // （如扩展区汉字的代理对）切成孤立代理，输出既失真又无法正确编码。
        int keepLen = 6;
        int codePoints = address.codePointCount(0, address.length());
        if (codePoints <= keepLen) {
            // 保留前缀已覆盖整串（原实现只在 <=3 时整体掩码），此时掩码形同虚设，全掩码处理
            return StrConst.MASK_REPLACEMENT;
        }
        return address.substring(0, address.offsetByCodePoints(0, keepLen)) + StrConst.MASK_REPLACEMENT;
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
        // 按码点保留首字、按码点计数掩码：charAt(0)/length() 面向 UTF-16 单元，
        // 扩展区汉字（代理对）会被从中间切开，产生无法正确编码的孤立代理
        int codePoints = chineseName.codePointCount(0, chineseName.length());
        return chineseName.substring(0, chineseName.offsetByCodePoints(0, 1))
                + "*".repeat(Math.max(1, codePoints - 1));
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
        // 归一化空白，避免车牌内部空格干扰识别与掩码
        String compact = carLicense.replaceAll("\\s", StrConst.EMPTY);
        if (!ValidateUtil.isCarLicense(compact)) {
            // 无法识别为车牌号时保守全掩码，避免原样输出造成信息泄漏
            return StrConst.MASK_REPLACEMENT;
        }
        return compact.substring(0, 2) + "****" + compact.substring(compact.length() - 1);
    }
}
