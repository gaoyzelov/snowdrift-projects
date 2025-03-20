package com.snowdrift.core.constant;

/**
 * RegexConst
 *
 * @author gaoye
 * @date 2025/03/18 16:46:16
 * @description 正则常量
 * @since 1.0
 */
public interface RegexConst {

    String URL = "^https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?";
    String PHONE = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
    String CARD_NO = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[Xx\\d]$";
    String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    String DATA_SIZE = "^\\d+(\\.\\d+)?(B|KB|MB|GB|TB|PB)$";
    String DATE_TIME = "\\d{4}-\\d{1,2}-\\d{1,2}(\\s\\d{1,2}:\\d{1,2}(:\\d{1,2})?(.\\d{1,6})?)?";
    String SPEL = "^#.*.$";
}