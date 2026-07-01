package com.snowdrift.framework.common.util;

import com.snowdrift.framework.common.constant.RegexConst;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ValidateUtil
 *
 * @author gaoyzelov
 * @date 2026/3/31-17:31
 * @description 验证工具类
 * @since 1.0.0
 */
public final class ValidateUtil {

    // 地区编码
    private static final Set<String> AREA_SET = SetUtils.hashSet(
            "11", "12", "13", "14", "15", "21", "22", "23", "31", "32", "33", "34", "35", "36", "37", "41",
            "42", "43", "44", "45", "46", "50", "51", "52", "53", "54", "61", "62", "63", "64", "65", "71",
            "81", "82", "91");
    // 校验码权重
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    // 校验码对应值
    private static final String[] CHECK_CODE = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

    private ValidateUtil() {
    }

    /**
     * 验证 URL 地址格式
     *
     * @param url 待验证的 URL 地址
     * @return true-格式正确，false-格式错误
     */
    public static boolean isURL(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        return Pattern.matches(RegexConst.URL,url);
    }

    /**
     * 验证车架号（VIN 码）
     *
     * @param vin 待验证的车架号
     * @return true-格式正确，false-格式错误
     */
    public static boolean isVIN(String vin) {
        if (StringUtils.isBlank(vin)) {
            return false;
        }
        return Pattern.matches(RegexConst.VIN,vin);
    }

    /**
     * 验证银行卡号（支持空格分隔）
     *
     * @param bankCard 待验证的银行卡号
     * @return true-格式正确，false-格式错误
     */
    public static boolean isBankCard(String bankCard) {
        if (StringUtils.isBlank(bankCard)) {
            return false;
        }
        return Pattern.matches(RegexConst.BANK_CARD,bankCard);
    }

    /**
     * 校验车牌号
     *
     * @param carLicense 车牌号
     * @return true/false
     */
    public static boolean isCarLicense(String carLicense) {
        if (StringUtils.isBlank(carLicense)) {
            return false;
        }
        return Pattern.matches(RegexConst.CAR_LICENSE, carLicense);
    }

    /**
     * 校验手机号
     *
     * @param mobilePhone 手机号
     * @return true/false
     */
    public static boolean isMobilePhone(String mobilePhone) {
        if (StringUtils.isBlank(mobilePhone)) {
            return false;
        }
        return Pattern.matches(RegexConst.MOBILE_PHONE, mobilePhone);
    }

    /**
     * 校验固定电话
     *
     * @param fixedPhone 固定电话
     * @return true/false
     */
    public static boolean isFixedPhone(String fixedPhone) {
        if (StringUtils.isBlank(fixedPhone)) {
            return false;
        }
        return Pattern.matches(RegexConst.FIXED_PHONE, fixedPhone);
    }

    /**
     * 校验身份证号
     *
     * @param idCard 身份证号
     * @return true/false
     */
    public static boolean isIdCard(String idCard) {
        if (StringUtils.isBlank(idCard) || (idCard.length() != 15 && idCard.length() != 18)) {
            return false;
        }
        boolean matches = Pattern.matches(RegexConst.CARD_NO, idCard);
        if (!matches) {
            return false;
        }
        int cardLength = idCard.length();
        // 校验地区码
        String areaCode = idCard.substring(0, 2);
        if (!AREA_SET.contains(areaCode)) {
            return false;
        }
        // 校验出生年月日
        String yearStr = cardLength == 15 ? "19" + idCard.substring(6, 8) : idCard.substring(6, 10);
        int year = Integer.parseInt(yearStr);
        if (year < 1900 || year > LocalDate.now().getYear()) {
            return false;
        }
        String monthStr = cardLength == 15 ? idCard.substring(8, 10) : idCard.substring(10, 12);
        int month = Integer.parseInt(monthStr);
        if (month < 1 || month > 12) {
            return false;
        }
        String dayStr = cardLength == 15 ? idCard.substring(10, 12) : idCard.substring(12, 14);
        int day = Integer.parseInt(dayStr);
        if (day < 1 || day > 31) {
            return false;
        }
        // 合法日期校验
        String date = yearStr + monthStr + dayStr;
        try {
            DateTimeUtil.parseLocalDate(date, "yyyyMMdd");
        } catch (Exception e) {
            return false;
        }
        // 校验校验码
        if (cardLength == 15) {
            return true;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            sum += (c - '0') * WEIGHT[i];
        }
        int mod = sum % 11;
        String checkCode = CHECK_CODE[mod];
        return StringUtils.equalsIgnoreCase(checkCode, idCard.substring(17));
    }

    /**
     * 校验邮箱
     *
     * @param email 邮箱
     * @return true/false
     */
    public static boolean isEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return Pattern.matches(RegexConst.EMAIL, email);
    }

}
