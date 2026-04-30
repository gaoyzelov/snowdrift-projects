package com.snowdrift.framework.common.util;

import com.snowdrift.framework.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DateTimeUtil
 *
 * @author 83674
 * @date 2026/3/30-10:02
 * @description 时间工具类
 * @since 1.0.0
 */
@Slf4j
public final class DateTimeUtil {

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private DateTimeUtil() {
    }

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    public static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    public static final String CHINESE_DATETIME_PATTERN = "yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒";

    public static final String CHINESE_DATE_PATTERN = "yyyy 年 MM 月 dd 日";

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    /**
     * 获取已缓存的 DateTimeFormatter（带缓存机制）
     *
     * @param format 格式模式
     * @return DateTimeFormatter 对象
     */
    private static DateTimeFormatter getFormatter(String format) {
        AssertUtil.notBlank(format, "格式不能为空");
        return FORMATTER_CACHE.computeIfAbsent(format, DateTimeFormatter::ofPattern);
    }

    /**
     * 获取时间字符串
     *
     * @param dateTime 时间
     * @param format   时间格式
     * @return 时间字符串
     */
    public static String getDateTimeString(LocalDateTime dateTime, String format) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        AssertUtil.notBlank(format, "格式不能为空");
        DateTimeFormatter formatter = getFormatter(format);
        return dateTime.format(formatter);
    }

    /**
     * 将 LocalDateTime 使用预定义 Formatter 格式化为字符串
     *
     * @param dateTime  待格式化的时间
     * @param formatter 预定义的 DateTimeFormatter
     */
    public static String getDateTimeString(LocalDateTime dateTime, DateTimeFormatter formatter) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        AssertUtil.notNull(formatter, "格式化器不能为空");
        return dateTime.format(formatter);
    }

    /**
     * 将 LocalDateTime 格式化为默认格式（yyyy-MM-dd HH:mm:ss）的字符串
     *
     * @param dateTime 待格式化的时间
     */
    public static String getDateTimeString(LocalDateTime dateTime) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        return getDateTimeString(dateTime, DATETIME_FORMATTER);
    }

    /**
     * 将字符串解析为 LocalDateTime
     *
     * @param dateTimeStr 时间字符串
     * @param pattern     时间格式图案
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr, String pattern) {
        AssertUtil.notBlank(dateTimeStr, "时间字符串不能为空");
        AssertUtil.notNull(pattern, "格式不能为空");
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.error("解析时间失败：{}", dateTimeStr, e);
            throw new BizException("解析时间失败: " + dateTimeStr + " ,错误信息:" + e.getLocalizedMessage());
        }
    }

    /**
     * 使用预定义 Formatter 将字符串解析为 LocalDateTime
     *
     * @param dateTimeStr 时间字符串
     * @param formatter   预定义的 DateTimeFormatter
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr, DateTimeFormatter formatter) {
        AssertUtil.notBlank(dateTimeStr, "时间字符串不能为空");
        AssertUtil.notNull(formatter, "格式化器不能为空");
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.error("解析时间失败：{}", dateTimeStr, e);
            throw new BizException("解析时间失败: " + dateTimeStr + " ,错误信息:" + e.getLocalizedMessage());
        }
    }

    /**
     * 将字符串解析为 LocalDateTime（默认格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTimeStr 时间字符串
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr) {
        AssertUtil.notBlank(dateTimeStr, "时间字符串不能为空");
        return parseLocalDateTime(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * 将毫秒时间戳转换为 LocalDateTime
     *
     * @param timestamp 毫秒时间戳
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳
     *
     * @param dateTime 待转换的时间
     */
    public static long localDateTimeToTimestamp(LocalDateTime dateTime) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        return dateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * 将 LocalDateTime 转换为 Date
     *
     * @param dateTime 待转换的时间
     * @return 转换后的 Date
     * @throws BizException 当 dateTime 为 null 时抛出
     */
    public static Date localDateTimeToDate(LocalDateTime dateTime) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        return Date.from(dateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * 将 Date 转换为 LocalDateTime
     *
     * @param date 待转换的日期
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        AssertUtil.notNull(date, "时间不能为空");
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * 判断时间是否在指定时间段内（包含边界）
     *
     * @param dateTime 待判断的时间
     * @param start    开始时间
     * @param end      结束时间
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * 计算两个时间之间的年数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 年数差（可能为负数）
     */
    public static long betweenYears(LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return ChronoUnit.YEARS.between(start, end);
    }

    /**
     * 计算两个时间之间的月数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 月数差（可能为负数）
     */
    public static long betweenMonths(LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return ChronoUnit.MONTHS.between(start, end);
    }

    /**
     * 计算两个时间之间的天数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 天数差（可能为负数）
     */
    public static long betweenDays(LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个时间之间的小时数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时数差（可能为负数）
     */
    public static long betweenHours(LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个时间之间的分钟数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟数差（可能为负数）
     */
    public static long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个时间之间的秒数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒数差（可能为负数）
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 获取指定日期的开始时间（00:00:00）
     *
     * @param date 日期
     */
    public static LocalDateTime getStartOfDay(LocalDate date) {
        if (null == date) {
            date = LocalDate.now();
        }
        return date.atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间（23:59:59.999999999）
     *
     * @param date 日期
     */
    public static LocalDateTime getEndOfDay(LocalDate date) {
        if (null == date) {
            date = LocalDate.now();
        }
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 将 LocalDate 格式化为指定格式的字符串
     *
     * @param date 待格式化的日期
     */
    public static String getDateString(LocalDate date, String format) {
        AssertUtil.notNull(date, "日期不能为空");
        AssertUtil.notNull(format, "格式不能为空");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    /**
     * 将 LocalDate 格式化为日期字符串（yyyy-MM-dd）
     *
     * @param date 待格式化的日期
     */
    public static String getDateString(LocalDate date) {
        AssertUtil.notNull(date, "日期不能为空");
        return date.format(DATE_FORMATTER);
    }

    /**
     * 将字符串解析为 LocalDate
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式图案
     */
    public static LocalDate parseLocalDate(String dateStr, String pattern) {
        AssertUtil.notNull(dateStr, "日期不能为空");
        AssertUtil.notNull(pattern, "格式不能为空");
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.error("解析日期失败：{}", dateStr, e);
            throw new BizException("解析日期失败: " + dateStr + " ,错误信息:" + e.getLocalizedMessage());
        }
    }

    /**
     * 使用预定义 Formatter 将字符串解析为 LocalDate
     *
     * @param dateStr   时间字符串
     * @param formatter 预定义的 DateTimeFormatter
     */
    public static LocalDate parseLocalDate(String dateStr, DateTimeFormatter formatter) {
        AssertUtil.notNull(dateStr, "日期不能为空");
        AssertUtil.notNull(formatter, "格式化器不能为空");
        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.error("解析日期失败：{}", dateStr, e);
            throw new BizException("解析日期失败: " + dateStr + " ,错误信息:" + e.getLocalizedMessage());
        }
    }

    /**
     * 将字符串解析为 LocalDate（默认格式：yyyy-MM-dd）
     *
     * @param dateStr 日期字符串
     */
    public static LocalDate parseLocalDate(String dateStr) {
        AssertUtil.notNull(dateStr, "日期不能为空");
        return parseLocalDate(dateStr, DATE_FORMATTER);
    }

    /**
     * 将 Date 转换为 LocalDate
     *
     * @param date 待转换的日期
     */
    public static LocalDate dateToLocalDate(Date date) {
        AssertUtil.notNull(date, "日期不能为空");
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDate();
    }

    /**
     * 判断日期是否在指定日期段内（包含边界）
     *
     * @param date  待判断的日期
     * @param start 开始日期
     * @param end   结束日期
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        AssertUtil.notNull(start, "开始日期不能为空");
        AssertUtil.notNull(end, "结束日期不能为空");
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 天数差（可能为负数）
     */
    public static long betweenDays(LocalDate start, LocalDate end) {
        AssertUtil.notNull(start, "开始日期不能为空");
        AssertUtil.notNull(end, "结束日期不能为空");
        return ChronoUnit.DAYS.between(start, end);
    }


    /**
     * 将 LocalTime 格式化为指定格式的字符串
     *
     * @param time 待格式化的时间
     */
    public static String getTimeString(LocalTime time, String format) {
        AssertUtil.notNull(time, "时间不能为空");
        AssertUtil.notBlank(format, "格式不能为空");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return time.format(formatter);
    }

    /**
     * 将 LocalTime 格式化为时间字符串（HH:mm:ss）
     *
     * @param time 待格式化的时间
     */
    public static String getTimeString(LocalTime time) {
        AssertUtil.notNull(time, "时间不能为空");
        return time.format(TIME_FORMATTER);
    }

    /**
     * 将字符串解析为 LocalTime
     *
     * @param timeStr 时间字符串
     * @param pattern 时间格式图案
     */
    public static LocalTime parseLocalTime(String timeStr, String pattern) {
        AssertUtil.notBlank(timeStr, "时间不能为空");
        AssertUtil.notBlank(pattern, "格式不能为空");
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalTime.parse(timeStr, formatter);
        } catch (Exception e) {
            log.error("解析时间失败：{}", timeStr, e);
            throw new BizException("解析时间失败: " + timeStr + " ,错误信息:" + e.getLocalizedMessage());
        }
    }

    /**
     * 使用预定义 Formatter 将字符串解析为 LocalTime
     *
     * @param timeStr   时间字符串
     * @param formatter 预定义的 DateTimeFormatter
     */
    public static LocalTime parseLocalTime(String timeStr, DateTimeFormatter formatter) {
        AssertUtil.notBlank(timeStr, "时间不能为空");
        AssertUtil.notNull(formatter, "格式化器不能为空");
        try {
            return LocalTime.parse(timeStr, formatter);
        } catch (Exception e) {
            log.error("解析时间失败：{}", timeStr, e);
            throw new BizException("解析时间失败: " + timeStr + " ,错误信息:" + e.getLocalizedMessage());
        }
    }

    /**
     * 将字符串解析为 LocalTime（默认格式：HH:mm:ss）
     *
     * @param timeStr 时间字符串
     * @return 解析后的 LocalTime
     */
    public static LocalTime parseLocalTime(String timeStr) {
        AssertUtil.notBlank(timeStr, "时间不能为空");
        return parseLocalTime(timeStr, TIME_FORMATTER);
    }


    /**
     * 获取星期几
     *
     * @param dateTime 时间
     * @return DayOfWeek 枚举值（MONDAY-SUNDAY）
     */
    public static DayOfWeek getDayOfWeek(LocalDateTime dateTime) {
        if (null == dateTime) {
            dateTime = LocalDateTime.now();
        }
        return dateTime.getDayOfWeek();
    }

}
