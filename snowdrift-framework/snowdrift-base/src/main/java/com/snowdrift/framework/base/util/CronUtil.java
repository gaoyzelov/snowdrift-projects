package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.constant.StrConst;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;

/**
 * CronUtil
 *
 * @author gaoyzelov
 * @date 2026/3/30-11:40
 * @description cron工具类
 * @since 1.0.0
 */
public final class CronUtil {

    private static final String CRON_FORMAT = "ss mm HH dd MM ? yyyy";
    public static final String EVERY_MINUTE = "0 * * * * ?";
    public static final String EVERY_HOUR = "0 0 * * * ?";
    public static final String EVERY_DAY = "0 0 0 * * ?";
    public static final String EVERY_WEEK = "0 0 0 ? * MON";
    public static final String EVERY_MONTH = "0 0 0 1 * ?";
    public static final String EVERY_YEAR = "0 0 0 1 1 ?";

    /**
     * Quartz 星期缩写映射（SUN~SAT → 1~7）
     */
    private static final Map<String, Integer> DAY_NAMES = Map.ofEntries(
            Map.entry("SUN", 1), Map.entry("MON", 2), Map.entry("TUE", 3),
            Map.entry("WED", 4), Map.entry("THU", 5), Map.entry("FRI", 6), Map.entry("SAT", 7)
    );

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
        AssertUtil.predicate(() -> dateTime.getYear() >= 1970 && dateTime.getYear() <= 2099,
                "年份必须在1970到2099之间");
        return DateTimeUtil.getDateTimeString(dateTime, CRON_FORMAT);
    }

    /**
     * 验证 Quartz cron 基础子集，不是 Spring cron 或完整 Quartz 校验器。
     * 支持 6/7 段、数字、星号、列表、非跨界范围、步长和星期缩写；
     * 年仅支持 1970~2099 的单值或星号，不支持月名、L/W/#、宏。
     * false 也可能表示超出本工具支持的语法，提交调度器前应使用后端原生校验器。
     *
     * @param cronExpression cron 表达式
     * @return true-有效，false-无效
     */
    public static boolean isValid(String cronExpression) {
        if (StringUtils.isBlank(cronExpression)) {
            return false;
        }

        String[] fields = cronExpression.trim().split("\\s+");
        // 支持 6 段（秒 分 时 日 月 周）与 7 段（秒 分 时 日 月 周 年）两种写法
        if (fields.length < 6 || fields.length > 7) {
            return false;
        }

        try {
            for (int i = 0; i < fields.length; i++) {
                String field = fields[i];
                // '?' 仅允许出现在日(下标3)与周(下标5)字段，表示该字段不参与匹配
                if (field.contains(StrConst.QUESTION) && i != 3 && i != 5) {
                    return false;
                }
                if (!isValidField(field, i)) {
                    return false;
                }
            }
            // Quartz 子集要求日与周字段恰有一个为 '?'；不适用于 Spring cron。
            return fields[3].equals(StrConst.QUESTION) != fields[5].equals(StrConst.QUESTION);
        } catch (Exception e) {
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
        if (StrConst.ANY.equals(field)) {
            return true;
        }
        // '?' 仅允许出现在日(3)/周(5)字段，且必须占满整个字段
        if (StrConst.QUESTION.equals(field)) {
            return fieldIndex == 3 || fieldIndex == 5;
        }

        if (field.contains(StrConst.QUESTION)) {
            return false;
        }

        // 星期字段（fieldIndex=5）支持 MON-SUN 缩写
        if (fieldIndex == 5) {
            return isValidDayOfWeekField(field);
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
            case 6: // 年（可选）
                return field.matches("[0-9]{4}") && Integer.parseInt(field) >= 1970
                        && Integer.parseInt(field) <= 2099;
            default:
                return false;
        }

        if (field.contains(StrConst.COMMA)) {
            String[] values = field.split(StrConst.COMMA, -1);
            for (String value : values) {
                if (!isValidRange(value, min, max)) {
                    return false;
                }
            }
        } else {
            return isValidRange(field, min, max);
        }

        return true;
    }

    /**
     * 验证星期字段（支持 MON-SUN 缩写和数字 1-7）
     */
    private static boolean isValidDayOfWeekField(String field) {
        if (field.contains(StrConst.COMMA)) {
            for (String part : field.split(StrConst.COMMA, -1)) {
                if (!isValidDayOfWeekRange(part)) {
                    return false;
                }
            }
            return true;
        }
        return isValidDayOfWeekRange(field);
    }

    /**
     * 验证星期字段的单个值/范围（支持 MON-SUN 缩写）
     */
    private static boolean isValidDayOfWeekRange(String value) {
        try {
            if (value.contains(StrConst.SLASH)) {
                String[] parts = value.split(StrConst.SLASH, -1);
                if (parts.length != 2) return false;
                if (!isValidDayOfWeekRange(parts[0]) || parts[0].contains(StrConst.SLASH)) return false;
                if (!parts[1].matches("[0-9]+")) return false;
                int step = Integer.parseInt(parts[1]);
                return step > 0 && step <= 7;
            } else if (value.contains(StrConst.MIDLINE)) {
                String[] parts = value.split(StrConst.MIDLINE, -1);
                if (parts.length != 2) return false;
                int start = resolveDayOfWeek(parts[0]);
                int end = resolveDayOfWeek(parts[1]);
                return start > 0 && end > 0 && start <= end;
            } else {
                if (StrConst.ANY.equals(value)) return true;
                return resolveDayOfWeek(value) > 0;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 将星期标记解析为数字（1-7），无法识别时返回 -1
     */
    private static int resolveDayOfWeek(String token) {
        Integer num = DAY_NAMES.get(token.toUpperCase(Locale.ROOT));
        if (num != null) return num;
        try {
            if (!token.matches("[0-9]+")) return -1;
            int n = Integer.parseInt(token);
            return (n >= 1 && n <= 7) ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
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
            if (value.contains(StrConst.SLASH)) {
                // 步长形式：起始可为 *、单值或 a-b 区间，起始边界与步长都需合法
                String[] parts = value.split(StrConst.SLASH, -1);
                if (parts.length != 2) {
                    return false;
                }
                // 步长需大于 0 且不超过字段上限，拒绝 */120（分钟）这类越界步长，与 everyMinutes/everyHours 保持一致
                if (!parts[1].matches("[0-9]+")) return false;
                int step = Integer.parseInt(parts[1]);
                return isRangePartValid(parts[0], min, max) && step > 0 && step <= max;
            }
            return isRangePartValid(value, min, max);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 校验范围字段的取值部分：支持 *、单值、a-b 区间，取值须落在 [min, max] 内
     */
    private static boolean isRangePartValid(String value, int min, int max) {
        if (StrConst.ANY.equals(value)) {
            return true;
        }
        if (value.contains(StrConst.MIDLINE)) {
            String[] parts = value.split(StrConst.MIDLINE, -1);
            if (parts.length != 2) {
                return false;
            }
            if (!parts[0].matches("[0-9]+") || !parts[1].matches("[0-9]+")) return false;
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            return start >= min && start <= max && end >= min && end <= max && start <= end;
        }
        if (!value.matches("[0-9]+")) return false;
        int num = Integer.parseInt(value);
        return num >= min && num <= max;
    }

    /**
     * 获取每隔 N 分钟执行的 cron 表达式
     *
     * @param minutes 间隔分钟数
     * @return cron 表达式
     */
    public static String everyMinutes(int minutes) {
        AssertUtil.predicate(() -> minutes > 0 && minutes <= 59, "分钟必须在 1 到 59 之间");
        return String.format(Locale.ROOT, "0 */%d * * * ?", minutes);
    }

    /**
     * 获取每隔 N 小时执行的 cron 表达式
     *
     * @param hours 间隔小时数
     * @return cron 表达式
     */
    public static String everyHours(int hours) {
        AssertUtil.predicate(() -> hours > 0 && hours <= 23, "小时必须在 1 到 23 之间");
        return String.format(Locale.ROOT, "0 0 */%d * * ?", hours);
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
        return String.format(Locale.ROOT, "%d %d %d * * ?", second, minute, hour);
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
        return String.format(Locale.ROOT, "%d %d %d ? * MON-FRI", second, minute, hour);
    }

    /**
     * 校验时间参数
     *
     * @param hour   小时 (0-23)
     * @param minute 分钟 (0-59)
     * @param second 秒 (0-59)
     */
    private static void validateTime(int hour, int minute, int second) {
        AssertUtil.predicate(() -> hour >= 0 && hour <= 23, "小时必须在 0 到 23 之间");
        AssertUtil.predicate(() -> minute >= 0 && minute <= 59, "分钟必须在 0 到 59 之间");
        AssertUtil.predicate(() -> second >= 0 && second <= 59, "秒必须在 0 到 59 之间");
    }
}