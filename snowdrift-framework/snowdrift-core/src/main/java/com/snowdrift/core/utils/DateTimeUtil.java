package com.snowdrift.core.utils;

import com.snowdrift.core.constant.DatePatternConst;
import com.snowdrift.core.exception.BaseException;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Locale;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.SECONDS;

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
    public static DateTimeFormatter createFormatter(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            pattern = DatePatternConst.DATETIME_PATTERN;
        }
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
    public static String getDateTimeStr(LocalDateTime dateTime, String pattern) {
        if (Objects.isNull(dateTime)) {
            dateTime = LocalDateTime.now();
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DatePatternConst.DATETIME_PATTERN;
        }
        return createFormatter(pattern).format(dateTime);
    }

    /**
     * 解析时间字符串
     *
     * @param timeStr 时间字符串
     * @param pattern 时间格式
     * @return 时间对象
     */
    public static LocalTime parseTime(String timeStr, String pattern) {
        if (StringUtils.isBlank(timeStr)) {
            throw new BaseException("时间不能为空");
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DatePatternConst.TIME_PATTERN;
        }
        return LocalTime.parse(timeStr, createFormatter(pattern));
    }

    /**
     * 解析日期字符串
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return 日期对象
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            throw new BaseException("日期不能为空");
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DatePatternConst.DATE_PATTERN;
        }
        return LocalDate.parse(dateStr, createFormatter(pattern));
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     日期时间格式
     * @return 日期时间对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (StringUtils.isBlank(dateTimeStr)) {
            throw new BaseException("日期时间不能为空");
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DatePatternConst.DATETIME_PATTERN;
        }
        return LocalDateTime.parse(dateTimeStr, createFormatter(pattern));
    }

    /**
     * 计算两个日期之间的间隔
     *
     * @param start 开始日期
     * @param end   结束日期
     * @param unit  时间单位,仅支持年、月、日
     * @return 两个日期之间的间隔
     */
    public static long between(LocalDate start, LocalDate end, ChronoUnit unit) {
        ValidateUtil.checkDateRange(start, end);
        if (Objects.isNull(unit)) {
            unit = ChronoUnit.DAYS;
        }
        return Period.between(start, end).get(unit);
    }

    /**
     * 计算两个时间之间的间隔
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @param unit  时间单位,仅支持秒、纳秒
     * @return 两个时间之间的间隔
     */
    public static long between(LocalTime start, LocalTime end, ChronoUnit unit) {
        ValidateUtil.checkTimeRange(start, end);
        if (Objects.isNull(unit)) {
            unit = SECONDS;
        }
        return Duration.between(start, end).get(unit);
    }

    /**
     * 计算两个日期时间之间的间隔
     *
     * @param start 开始日期时间
     * @param end   结束日期时间
     * @param unit  时间单位，仅支持秒、分、时、天
     * @return 两个日期时间之间的间隔
     */
    public static long between(LocalDateTime start, LocalDateTime end, ChronoUnit unit) {
        ValidateUtil.checkDateTimeRange(start, end);
        if (Objects.isNull(unit)) {
            unit = SECONDS;
        }
        Duration duration = Duration.between(start, end);
        return switch (unit) {
            case SECONDS -> duration.toSeconds();
            case MINUTES -> duration.toMinutes();
            case HOURS -> duration.toHours();
            case DAYS -> duration.toDays();
            default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        };
    }
}