package com.snowdrift.core.constant;

/**
 * DatePatternConst
 *
 * @author gaoye
 * @date 2025/03/19 11:28:15
 * @description 时间格式常量
 * @since 1.0.0
 */
public interface DatePatternConst {

    String YEAR_PATTERN = "yyyy";
    String MONTH_PATTERN = "yyyy-MM";
    String SIMPLE_MONTH_PATTERN = "yyyyMM";
    String DATE_PATTERN = "yyyy-MM-dd";
    String TIME_PATTERN = "HH:mm:ss";
    String DATETIME_MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
    String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    String DATETIME_MS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";
    String CHINESE_DATE_PATTERN = "yyyy年MM月dd日";
    String CHINESE_DATE_TIME_PATTERN = "yyyy年MM月dd日HH时mm分ss秒";
    String PURE_DATE_PATTERN = "yyyyMMdd";
    String PURE_TIME_PATTERN = "HHmmss";
    String PURE_DATETIME_PATTERN = "yyyyMMddHHmmss";
    String PURE_DATETIME_MS_PATTERN = "yyyyMMddHHmmssSSS";
    String HTTP_DATETIME_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
    String JDK_DATETIME_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";
    String UTC_SIMPLE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    String UTC_SIMPLE_MS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    String UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    String UTC_WITH_ZONE_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";
    String UTC_WITH_XXX_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    String UTC_MS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    String UTC_MS_WITH_ZONE_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    String UTC_MS_WITH_XXX_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
}