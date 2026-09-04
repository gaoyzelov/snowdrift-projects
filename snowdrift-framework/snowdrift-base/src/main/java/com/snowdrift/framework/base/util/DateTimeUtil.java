package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DateTimeUtil
 *
 * @author gaoyzelov
 * @date 2026/3/30-10:02
 * @description 时间工具类：基于 java.time 的格式化/解析/转换/区间判断工具
 * @since 1.0.0
 */
@Slf4j
public final class DateTimeUtil {

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    public static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    public static final String CHINESE_DATETIME_PATTERN = "yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒";

    public static final String CHINESE_DATE_PATTERN = "yyyy 年 MM 月 dd 日";

    /** 系统默认时区，参与 Date/时间戳 与 LocalXxx 的互相转换 */
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    private DateTimeUtil() {
    }

    /**
     * 获取已缓存的 DateTimeFormatter（带缓存机制）
     *
     * @param pattern 格式模式
     * @return DateTimeFormatter 对象
     */
    private static DateTimeFormatter getFormatter(String pattern) {
        AssertUtil.notBlank(pattern, "格式不能为空");
        try {
            return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
        } catch (IllegalArgumentException e) {
            throw new BizException("时间格式非法：" + pattern, e);
        }
    }

    // ============================== 时间格式化 ==============================

    /**
     * 将 LocalDateTime 按指定格式模式格式化为字符串
     *
     * @param dateTime 时间
     * @param pattern  时间格式模式
     * @return 格式化后的字符串
     */
    public static String getDateTimeString(LocalDateTime dateTime, String pattern) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        DateTimeFormatter formatter = getFormatter(pattern);
        return dateTime.format(formatter);
    }

    /**
     * 将 LocalDateTime 使用预定义 DateTimeFormatter 格式化为字符串
     *
     * @param dateTime  待格式化的时间
     * @param formatter 预定义的 DateTimeFormatter
     * @return 格式化后的字符串
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
     * @return 格式化后的字符串
     */
    public static String getDateTimeString(LocalDateTime dateTime) {
        return getDateTimeString(dateTime, DATETIME_FORMATTER);
    }

    /**
     * 将 LocalDate 按指定格式模式格式化为字符串
     *
     * @param date   待格式化的日期
     * @param pattern 日期格式模式
     * @return 格式化后的字符串
     */
    public static String getDateString(LocalDate date, String pattern) {
        AssertUtil.notNull(date, "日期不能为空");
        DateTimeFormatter formatter = getFormatter(pattern);
        return date.format(formatter);
    }

    /**
     * 将 LocalDate 格式化为默认日期字符串（yyyy-MM-dd）
     *
     * @param date 待格式化的日期
     * @return 格式化后的字符串
     */
    public static String getDateString(LocalDate date) {
        AssertUtil.notNull(date, "日期不能为空");
        return date.format(DATE_FORMATTER);
    }

    /**
     * 将 LocalTime 按指定格式模式格式化为字符串
     *
     * @param time   待格式化的时间
     * @param pattern 时间格式模式
     * @return 格式化后的字符串
     */
    public static String getTimeString(LocalTime time, String pattern) {
        AssertUtil.notNull(time, "时间不能为空");
        DateTimeFormatter formatter = getFormatter(pattern);
        return time.format(formatter);
    }

    /**
     * 将 LocalTime 格式化为默认时间字符串（HH:mm:ss）
     *
     * @param time 待格式化的时间
     * @return 格式化后的字符串
     */
    public static String getTimeString(LocalTime time) {
        AssertUtil.notNull(time, "时间不能为空");
        return time.format(TIME_FORMATTER);
    }

    // ============================== 时间解析 ==============================

    /**
     * 解析内核：统一空值校验、异常日志与异常包装，保证所有解析失败都抛出携带原因链的 BizException
     *
     * @param text       待解析文本
     * @param formatter  格式化器
     * @param query      解析结果类型（LocalDateTime/LocalDate/LocalTime 的 from 查询）
     * @param subjectName 解析对象名称，用于错误信息（如 时间/日期）
     * @param <T>        解析结果类型
     * @return 解析后的时间对象
     */
    private static <T extends TemporalAccessor> T doParse(String text, DateTimeFormatter formatter,
                                                          TemporalQuery<T> query, String subjectName) {
        AssertUtil.notBlank(text, subjectName + "字符串不能为空");
        AssertUtil.notNull(formatter, "格式化器不能为空");
        AssertUtil.notNull(query, "解析器不能为空");
        try {
            return formatter.parse(text, query);
        } catch (DateTimeException e) {
            log.error("{}解析失败：{}", subjectName, text, e);
            throw new BizException(subjectName + "解析失败，请检查输入内容", e);
        }
    }

    /**
     * 将字符串按指定格式模式解析为 LocalDateTime
     *
     * @param dateTimeStr 时间字符串
     * @param pattern     时间格式模式
     * @return 解析后的 LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr, String pattern) {
        DateTimeFormatter formatter = getFormatter(pattern);
        return doParse(dateTimeStr, formatter, LocalDateTime::from, "时间");
    }

    /**
     * 将字符串使用预定义 DateTimeFormatter 解析为 LocalDateTime
     *
     * @param dateTimeStr 时间字符串
     * @param formatter   预定义的 DateTimeFormatter
     * @return 解析后的 LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr, DateTimeFormatter formatter) {
        return doParse(dateTimeStr, formatter, LocalDateTime::from, "时间");
    }

    /**
     * 将字符串解析为 LocalDateTime（默认格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTimeStr 时间字符串
     * @return 解析后的 LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String dateTimeStr) {
        return doParse(dateTimeStr, DATETIME_FORMATTER, LocalDateTime::from, "时间");
    }

    /**
     * 将字符串按指定格式模式解析为 LocalDate
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式模式
     * @return 解析后的 LocalDate
     */
    public static LocalDate parseLocalDate(String dateStr, String pattern) {
        DateTimeFormatter formatter = getFormatter(pattern);
        return doParse(dateStr, formatter, LocalDate::from, "日期");
    }

    /**
     * 将字符串使用预定义 DateTimeFormatter 解析为 LocalDate
     *
     * @param dateStr   日期字符串
     * @param formatter 预定义的 DateTimeFormatter
     * @return 解析后的 LocalDate
     */
    public static LocalDate parseLocalDate(String dateStr, DateTimeFormatter formatter) {
        return doParse(dateStr, formatter, LocalDate::from, "日期");
    }

    /**
     * 将字符串解析为 LocalDate（默认格式：yyyy-MM-dd）
     *
     * @param dateStr 日期字符串
     * @return 解析后的 LocalDate
     */
    public static LocalDate parseLocalDate(String dateStr) {
        return doParse(dateStr, DATE_FORMATTER, LocalDate::from, "日期");
    }

    /**
     * 将字符串按指定格式模式解析为 LocalTime
     *
     * @param timeStr 时间字符串
     * @param pattern 时间格式模式
     * @return 解析后的 LocalTime
     */
    public static LocalTime parseLocalTime(String timeStr, String pattern) {
        DateTimeFormatter formatter = getFormatter(pattern);
        return doParse(timeStr, formatter, LocalTime::from, "时间");
    }

    /**
     * 将字符串使用预定义 DateTimeFormatter 解析为 LocalTime
     *
     * @param timeStr   时间字符串
     * @param formatter 预定义的 DateTimeFormatter
     * @return 解析后的 LocalTime
     */
    public static LocalTime parseLocalTime(String timeStr, DateTimeFormatter formatter) {
        return doParse(timeStr, formatter, LocalTime::from, "时间");
    }

    /**
     * 将字符串解析为 LocalTime（默认格式：HH:mm:ss）
     *
     * @param timeStr 时间字符串
     * @return 解析后的 LocalTime
     */
    public static LocalTime parseLocalTime(String timeStr) {
        return doParse(timeStr, TIME_FORMATTER, LocalTime::from, "时间");
    }

    // ============================== 时间转换 ==============================

    /**
     * 将毫秒时间戳转换为 LocalDateTime（系统默认时区）
     *
     * @param timestamp 毫秒时间戳
     * @return 转换后的 LocalDateTime
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳（系统默认时区）
     *
     * @param dateTime 待转换的时间
     * @return 毫秒时间戳
     */
    public static long localDateTimeToTimestamp(LocalDateTime dateTime) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        return dateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * 将 LocalDateTime 转换为 Date（系统默认时区）
     *
     * @param dateTime 待转换的时间
     * @return 转换后的 Date
     */
    public static Date localDateTimeToDate(LocalDateTime dateTime) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        return Date.from(dateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * 将 Date 转换为 LocalDateTime（系统默认时区）
     *
     * @param date 待转换的日期时间
     * @return 转换后的 LocalDateTime
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        AssertUtil.notNull(date, "时间不能为空");
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * 将 Date 转换为 LocalDate（系统默认时区）
     *
     * @param date 待转换的日期
     * @return 转换后的 LocalDate
     */
    public static LocalDate dateToLocalDate(Date date) {
        AssertUtil.notNull(date, "日期不能为空");
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDate();
    }

    // ============================== 时间区间与边界 ==============================

    /**
     * 判断 LocalDateTime 是否在指定时间段内（包含边界）
     *
     * @param dateTime 待判断的时间
     * @param start    开始时间
     * @param end      结束时间
     * @return true-在区间内（含边界），false-不在区间内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * 判断 LocalDate 是否在指定日期段内（包含边界）
     *
     * @param date  待判断的日期
     * @param start 开始日期
     * @param end   结束日期
     * @return true-在区间内（含边界），false-不在区间内
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        AssertUtil.notNull(date, "日期不能为空");
        AssertUtil.notNull(start, "开始日期不能为空");
        AssertUtil.notNull(end, "结束日期不能为空");
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 计算两个 LocalDateTime 之间按指定单位的时间差
     *
     * @param unit  时间单位
     * @param start 开始时间
     * @param end   结束时间
     * @return 时间差（结束时间早于开始时间为负）
     */
    private static long between(ChronoUnit unit, LocalDateTime start, LocalDateTime end) {
        AssertUtil.notNull(start, "开始时间不能为空");
        AssertUtil.notNull(end, "结束时间不能为空");
        return unit.between(start, end);
    }

    /**
     * 计算两个时间之间的年数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 年数差（可能为负数）
     */
    public static long betweenYears(LocalDateTime start, LocalDateTime end) {
        return between(ChronoUnit.YEARS, start, end);
    }

    /**
     * 计算两个时间之间的月数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 月数差（可能为负数）
     */
    public static long betweenMonths(LocalDateTime start, LocalDateTime end) {
        return between(ChronoUnit.MONTHS, start, end);
    }

    /**
     * 计算两个时间之间的天数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 天数差（可能为负数）
     */
    public static long betweenDays(LocalDateTime start, LocalDateTime end) {
        return between(ChronoUnit.DAYS, start, end);
    }

    /**
     * 计算两个时间之间的小时数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时数差（可能为负数）
     */
    public static long betweenHours(LocalDateTime start, LocalDateTime end) {
        return between(ChronoUnit.HOURS, start, end);
    }

    /**
     * 计算两个时间之间的分钟数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟数差（可能为负数）
     */
    public static long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        return between(ChronoUnit.MINUTES, start, end);
    }

    /**
     * 计算两个时间之间的秒数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒数差（可能为负数）
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        return between(ChronoUnit.SECONDS, start, end);
    }

    /**
     * 计算两个 LocalDate 之间的天数差
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
     * 获取指定日期的开始时间（当天 00:00:00）；date 为空时取当前日期
     *
     * @param date 日期
     * @return 该日期 00:00:00
     */
    public static LocalDateTime getStartOfDay(LocalDate date) {
        if (Objects.isNull(date)) {
            date = LocalDate.now();
        }
        return date.atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间（当天 23:59:59.999999999）；date 为空时取当前日期
     *
     * @param date 日期
     * @return 该日期 23:59:59.999999999
     */
    public static LocalDateTime getEndOfDay(LocalDate date) {
        if (Objects.isNull(date)) {
            date = LocalDate.now();
        }
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 获取指定 LocalDateTime 的星期几；dateTime 为空时取当前时间
     *
     * @param dateTime 时间
     * @return DayOfWeek 枚举值（MONDAY-SUNDAY）
     */
    public static DayOfWeek getDayOfWeek(LocalDateTime dateTime) {
        if (Objects.isNull(dateTime)) {
            dateTime = LocalDateTime.now();
        }
        return dateTime.getDayOfWeek();
    }

}
