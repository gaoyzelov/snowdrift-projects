package com.snowdrift.framework.common.util;

import com.snowdrift.framework.common.constant.RegexConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * CronUtil
 *
 * @author gaoyzelov
 * @date 2026/3/30-11:40
 * @description cron工具类
 * @since 1.0.0
 */
@Slf4j
public final class CronUtil {

    private static final String CRON_FORMAT = "ss mm HH dd MM ? yyyy";

    private static final Pattern CRON_PATTERN = Pattern.compile(RegexConst.CRON);

    private CronUtil() {
    }

    /**
     * 根据 LocalDateTime 获取 cron 表达式
     *
     * @param dateTime 时间
     * @return cron 表达式（格式：秒 分 时 日 月 ？年）
     */
    public static String getCron(LocalDateTime dateTime) {
        AssertUtil.notNull(dateTime, "时间不能为空");
        return DateTimeUtil.getDateTimeString(dateTime, CRON_FORMAT);
    }

    /**
     * 验证 cron 表达式是否有效
     *
     * @param cronExpression cron 表达式
     * @return true-有效，false-无效
     */
    public static boolean isValid(String cronExpression) {
        if (StringUtils.isBlank(cronExpression)) {
            return false;
        }

        String[] fields = cronExpression.trim().split("\\s+");
        if (fields.length < 6 || fields.length > 7) {
            return false;
        }

        try {
            for (int i = 0; i < fields.length; i++) {
                String field = fields[i];
                if (!isValidField(field, i)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("验证 Cron 表达式失败：{}", cronExpression, e);
            return false;
        }
    }

    /**
     * 验证 cron 字段是否有效
     *
     * @param field      字段值
     * @param fieldIndex 字段索引 (0=秒，1=分，2=时，3=日，4=月，5=周，6=年)
     * @return true-有效，false-无效
     */
    private static boolean isValidField(String field, int fieldIndex) {
        if ("*".equals(field) || "?".equals(field)) {
            return true;
        }

        int min, max;
        switch (fieldIndex) {
            case 0: // 秒 0-59
                min = 0;
                max = 59;
                break;
            case 1: // 分 0-59
                min = 0;
                max = 59;
                break;
            case 2: // 时 0-23
                min = 0;
                max = 23;
                break;
            case 3: // 日 1-31
                min = 1;
                max = 31;
                break;
            case 4: // 月 1-12
                min = 1;
                max = 12;
                break;
            case 5: // 周 1-7 或 ?
                min = 1;
                max = 7;
                break;
            case 6: // 年（可选）
                return field.matches("\\d{4}");
            default:
                return false;
        }

        if (field.contains(",")) {
            String[] values = field.split(",");
            for (String value : values) {
                if (!isValidRange(value, min, max)) {
                    return false;
                }
            }
        } else if (field.contains("/") || field.contains("-")) {
            return isValidRange(field, min, max);
        } else {
            return isValidRange(field, min, max);
        }

        return true;
    }

    /**
     * 验证范围值是否有效
     *
     * @param value 值
     * @param min   最小值
     * @param max   最大值
     * @return true-有效，false-无效
     */
    private static boolean isValidRange(String value, int min, int max) {
        try {
            if (value.contains("/")) {
                String[] parts = value.split("/");
                if (parts.length != 2) {
                    return false;
                }
                if (!"*".equals(parts[0]) && !parts[0].contains("-")) {
                    int num = Integer.parseInt(parts[0]);
                    if (num < min || num > max) {
                        return false;
                    }
                }
                int step = Integer.parseInt(parts[1]);
                return step > 0;
            } else if (value.contains("-")) {
                String[] parts = value.split("-");
                if (parts.length != 2) {
                    return false;
                }
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                return start >= min && start <= max && end >= min && end <= max && start <= end;
            } else {
                if ("*".equals(value)) {
                    return true;
                }
                int num = Integer.parseInt(value);
                return num >= min && num <= max;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取每分钟的 cron 表达式（每分钟第 0 秒执行）
     *
     * @return cron 表达式
     */
    public static String everyMinute() {
        return "0 * * * * ?";
    }

    /**
     * 获取每小时的 cron 表达式（每小时第 0 分第 0 秒执行）
     *
     * @return cron 表达式
     */
    public static String everyHour() {
        return "0 0 * * * ?";
    }

    /**
     * 获取每天的 cron 表达式（每天 0 点 0 分 0 秒执行）
     *
     * @return cron 表达式
     */
    public static String everyDay() {
        return "0 0 0 * * ?";
    }

    /**
     * 获取每周的 cron 表达式（每周一 0 点 0 分 0 秒执行）
     *
     * @return cron 表达式
     */
    public static String everyWeek() {
        return "0 0 0 ? * MON";
    }

    /**
     * 获取每月的 cron 表达式（每月 1 号 0 点 0 分 0 秒执行）
     *
     * @return cron 表达式
     */
    public static String everyMonth() {
        return "0 0 0 1 * ?";
    }

    /**
     * 获取每年 cron 表达式（每年 1 月 1 日 0 点 0 分 0 秒执行）
     *
     * @return cron 表达式
     */
    public static String everyYear() {
        return "0 0 0 1 1 ?";
    }

    /**
     * 获取每隔 N 分钟执行的 cron 表达式
     *
     * @param minutes 间隔分钟数
     * @return cron 表达式
     */
    public static String everyMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be greater than 0");
        }
        return String.format("0 */%d * * * ?", minutes);
    }

    /**
     * 获取每隔 N 小时执行的 cron 表达式
     *
     * @param hours 间隔小时数
     * @return cron 表达式
     */
    public static String everyHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be greater than 0");
        }
        return String.format("0 0 */%d * * ?", hours);
    }

    /**
     * 获取工作日 cron 表达式（周一至周五 9:00-18:00 整点执行）
     *
     * @return cron 表达式
     */
    public static String workDay() {
        return "0 0 9-18 ? * MON-FRI";
    }

    /**
     * 获取指定时间执行的 cron 表达式
     *
     * @param hour   小时 (0-23)
     * @param minute 分钟 (0-59)
     * @param second 秒 (0-59)
     * @return cron 表达式
     */
    public static String atTime(int hour, int minute, int second) {
        CronUtil.validateTime(hour, minute, second);
        return String.format("%d %d %d * * ?", second, minute, hour);
    }

    /**
     * 获取工作日指定时间 cron 表达式
     *
     * @param hour   小时 (0-23)
     * @param minute 分钟 (0-59)
     * @param second 秒 (0-59)
     * @return cron 表达式
     */
    public static String atWorkDayTime(int hour, int minute, int second) {
        CronUtil.validateTime(hour, minute, second);
        return String.format("%d %d %d ? * MON-FRI", second, minute, hour);
    }

    /**
     * 校验时间参数
     *
     * @param hour   小时 (0-23)
     * @param minute 分钟 (0-59)
     * @param second 秒 (0-59)
     */
    private static void validateTime(int hour, int minute, int second) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Minute must be between 0 and 59");
        }
        if (second < 0 || second > 59) {
            throw new IllegalArgumentException("Second must be between 0 and 59");
        }
    }
}