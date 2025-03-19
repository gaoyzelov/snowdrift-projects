package com.snowdrift.core.utils;

import com.snowdrift.core.constant.DatePatternConst;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DateTimeUtil
 *
 * @author gaoye
 * @date 2025/03/19 11:22:49
 * @description 时间工具类
 * @since 1.0
 */
public class DateTimeUtil {

    /**
     * 创建日期时间格式化对象
     *
     * @param pattern 日期时间格式
     * @return 日期时间格式化对象
     */
    public static DateTimeFormatter createFormatter(@NonNull String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withZone(ZoneId.systemDefault());
    }

    /**
     * 获取日期时间字符串
     *
     * @return 日期时间字符串
     */
    public static String getDateTimeStr() {
        return getDateTimeStr(LocalDateTime.now(), DatePatternConst.DATETIME_PATTERN);
    }

    /**
     * 获取日期时间字符串
     *
     * @param dateTime 日期时间
     * @param pattern  日期时间格式
     * @return 日期时间字符串
     */
    public static String getDateTimeStr(@NonNull LocalDateTime dateTime, @NonNull String pattern) {
        return createFormatter(pattern).format(dateTime);
    }
}