package com.snowdrift.framework.base.constant;

/**
 * RegexConst
 * @author gaoyzelov
 * @date 2026/3/31-17:33
 * @description 正则表达式常量
 * @since 1.0.0
 */
public final class RegexConst {

    private RegexConst() {
    }

    // 地址
    public static final String URL = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";

    // 身份证号
    public static final String CARD_NO = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[Xx\\d]$";

    // 移动电话（中国大陆 11 位，首位 1、次位 3-9；不做号段白名单，避免新增号段无法校验）
    public static final String MOBILE_PHONE = "^1[3-9]\\d{9}$";

    // 固定电话
    public static final String FIXED_PHONE = "^(\\d{3,4}-)?\\d{7,8}$";

    // 中文
    public static final String CHINESE = "^[\\u4e00-\\u9fa5]+$";

    // 邮箱
    public static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    //验证密码格式（8-15 位，必须包含字母和数字）
    public static final String PWD = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,15}$";

    //银行卡号（16/19 位，支持按 4 位分组、组间以单个空格分隔）
    public static final String BANK_CARD = "^\\d{4}( ?\\d{4}){3}( ?\\d{3})?$";

    //车架号
    public static final String VIN = "^[A-HJ-NPR-Z\\d]{17}$";

    // 车牌号（支持传统蓝牌、新能源绿牌、港澳车牌等）
    public static final String CAR_LICENSE = "^(([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z](([0-9]{5}[DF])|([DF]([A-HJ-NP-Z0-9])[0-9]{4})))|([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳使领]))$";

    // CRON表达式
    public static final String CRON = "^((((\\d+,)+\\d+)|(\\d+(\\/|-)\\d+)|\\d+|\\*) ?){5,7}$";

    // i18n 国际化
    public static final String I18N = "^[a-zA-Z0-9._-]+$";
}

