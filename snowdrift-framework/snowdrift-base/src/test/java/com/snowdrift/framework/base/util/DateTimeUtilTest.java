package com.snowdrift.framework.base.util;

import com.google.common.cache.Cache;
import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DateTimeUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 时间工具类单元测试
 * @since 1.0.0
 */
class DateTimeUtilTest {

    private static final LocalDateTime SAMPLE = LocalDateTime.of(2026, 9, 4, 12, 30, 45);

    @Nested
    @DisplayName("格式化")
    class Format {

        @Test
        @DisplayName("预定义格式与自定义 pattern 均可用")
        void formats() {
            assertThat(DateTimeUtil.getDateTimeString(SAMPLE)).isEqualTo("2026-09-04 12:30:45");
            assertThat(DateTimeUtil.getDateString(SAMPLE.toLocalDate())).isEqualTo("2026-09-04");
            assertThat(DateTimeUtil.getTimeString(SAMPLE.toLocalTime())).isEqualTo("12:30:45");

            assertThat(DateTimeUtil.getDateTimeString(SAMPLE, "yyyy/MM/dd HH:mm")).isEqualTo("2026/09/04 12:30");
            assertThat(DateTimeUtil.getDateTimeString(SAMPLE, DateTimeUtil.DATETIME_FORMATTER))
                    .isEqualTo("2026-09-04 12:30:45");
            assertThat(DateTimeUtil.getDateTimeString(SAMPLE, DateTimeUtil.TIMESTAMP_PATTERN))
                    .isEqualTo("20260904123045");
        }

        @Test
        @DisplayName("中文格式常量可用于格式化与解析往返")
        void chinesePatterns() {
            String dateTime = DateTimeUtil.getDateTimeString(SAMPLE, DateTimeUtil.CHINESE_DATETIME_PATTERN);
            assertThat(dateTime).isEqualTo("2026 年 09 月 04 日 12 时 30 分 45 秒");
            assertThat(DateTimeUtil.parseLocalDateTime(dateTime, DateTimeUtil.CHINESE_DATETIME_PATTERN))
                    .isEqualTo(SAMPLE);

            String date = DateTimeUtil.getDateString(SAMPLE.toLocalDate(), DateTimeUtil.CHINESE_DATE_PATTERN);
            assertThat(date).isEqualTo("2026 年 09 月 04 日");
            assertThat(DateTimeUtil.parseLocalDate(date, DateTimeUtil.CHINESE_DATE_PATTERN))
                    .isEqualTo(SAMPLE.toLocalDate());
        }

        @Test
        @DisplayName("时间类型与 pattern 不匹配时抛 BizException，而不是 JDK 的异常")
        void formatMismatchWrapsJdkException() {
            assertThatThrownBy(() -> DateTimeUtil.getTimeString(LocalTime.NOON, "yyyy-MM-dd"))
                    .isInstanceOf(BizException.class)
                    .hasMessage("时间格式化失败")
                    .hasCauseInstanceOf(java.time.DateTimeException.class);

            assertThatThrownBy(() -> DateTimeUtil.getDateString(LocalDate.now(), "HH:mm:ss"))
                    .isInstanceOf(BizException.class)
                    .hasMessage("日期格式化失败");

            // LocalDateTime 不带时区，用带时区的 pattern 同样落到 BizException
            assertThatThrownBy(() -> DateTimeUtil.getDateTimeString(SAMPLE, "yyyy-MM-dd HH:mm:ss z"))
                    .isInstanceOf(BizException.class)
                    .hasMessage("时间格式化失败");
        }

        @Test
        @DisplayName("空值与非法 pattern 的前置校验")
        void preconditions() {
            assertThatThrownBy(() -> DateTimeUtil.getDateTimeString((LocalDateTime) null))
                    .isInstanceOf(BizException.class).hasMessage("时间不能为空");
            assertThatThrownBy(() -> DateTimeUtil.getDateTimeString(SAMPLE, (String) null))
                    .isInstanceOf(BizException.class).hasMessage("格式不能为空");
            assertThatThrownBy(() -> DateTimeUtil.getDateTimeString(SAMPLE, "  "))
                    .isInstanceOf(BizException.class).hasMessage("格式不能为空");
            // 未闭合的单引号是非法 pattern（注意 "[" 是合法的可选段起始，会格式化为空串）
            assertThatThrownBy(() -> DateTimeUtil.getDateTimeString(SAMPLE, "'"))
                    .isInstanceOf(BizException.class).hasMessage("时间格式非法");
            assertThatThrownBy(() -> DateTimeUtil.getDateTimeString(SAMPLE, (DateTimeFormatter) null))
                    .isInstanceOf(BizException.class).hasMessage("格式化器不能为空");
        }
    }

    @Nested
    @DisplayName("解析")
    class Parse {

        @Test
        @DisplayName("默认格式与自定义 pattern 均可用")
        void parses() {
            assertThat(DateTimeUtil.parseLocalDateTime("2026-09-04 12:30:45")).isEqualTo(SAMPLE);
            assertThat(DateTimeUtil.parseLocalDate("2026-09-04")).isEqualTo(SAMPLE.toLocalDate());
            assertThat(DateTimeUtil.parseLocalTime("12:30:45")).isEqualTo(SAMPLE.toLocalTime());

            assertThat(DateTimeUtil.parseLocalDateTime("20260904123045", DateTimeUtil.TIMESTAMP_PATTERN))
                    .isEqualTo(SAMPLE);
            assertThat(DateTimeUtil.parseLocalDate("2026/09/04", "yyyy/MM/dd")).isEqualTo(SAMPLE.toLocalDate());
            assertThat(DateTimeUtil.parseLocalTime("12-30-45", "HH-mm-ss")).isEqualTo(SAMPLE.toLocalTime());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "2026-02-30 00:00:00",   // 不存在的日期
                "2025-02-29 00:00:00",   // 非闰年
                "2026-04-31 00:00:00",   // 小月 31 日
                "2026-13-01 00:00:00",   // 月份越界
                "2026-09-04 24:00:00",   // 小时越界
                "2026-09-04 12:60:00",   // 分钟越界
                "2026-09-04 12:30:60",   // 秒越界
                "2026-9-4 12:30:45",     // 非定宽，STRICT 下拒绝
                "not-a-datetime"})
        @DisplayName("STRICT 解析拒绝非法日期，包装为 BizException 并保留原因链")
        void strictParsingRejects(String text) {
            assertThatThrownBy(() -> DateTimeUtil.parseLocalDateTime(text))
                    .isInstanceOf(BizException.class)
                    .hasMessage("时间解析失败")
                    .hasCauseInstanceOf(java.time.DateTimeException.class);
        }

        @Test
        @DisplayName("合法闰日可通过")
        void leapDayAccepted() {
            assertThat(DateTimeUtil.parseLocalDate("2024-02-29")).isEqualTo(LocalDate.of(2024, 2, 29));
        }

        @Test
        @DisplayName("自定义 DateTimeFormatter 的宽松度由调用方决定")
        void customFormatterIsRespected() {
            DateTimeFormatter lenient = DateTimeFormatter.ofPattern("yyyy-M-d");
            assertThat(DateTimeUtil.parseLocalDate("2026-9-4", lenient)).isEqualTo(SAMPLE.toLocalDate());
        }

        @Test
        @DisplayName("空入参与空格式化器的前置校验")
        void preconditions() {
            assertThatThrownBy(() -> DateTimeUtil.parseLocalDateTime(null))
                    .isInstanceOf(BizException.class).hasMessage("时间字符串不能为空");
            assertThatThrownBy(() -> DateTimeUtil.parseLocalDate("  "))
                    .isInstanceOf(BizException.class).hasMessage("日期字符串不能为空");
            assertThatThrownBy(() -> DateTimeUtil.parseLocalTime("12:30:45", (DateTimeFormatter) null))
                    .isInstanceOf(BizException.class).hasMessage("格式化器不能为空");
        }
    }

    @Nested
    @DisplayName("格式化器缓存")
    class FormatterCache {

        @SuppressWarnings("unchecked")
        private Cache<Object, Object> cache() throws Exception {
            Field field = DateTimeUtil.class.getDeclaredField("FORMATTER_CACHE");
            field.setAccessible(true);
            return (Cache<Object, Object>) field.get(null);
        }

        @Test
        @DisplayName("同一 pattern 复用缓存条目，动态 pattern 不会无限增长（上限 256）")
        void boundedAndReused() throws Exception {
            Cache<Object, Object> cache = cache();
            cache.invalidateAll();

            DateTimeUtil.getDateTimeString(SAMPLE, "yyyy/MM/dd");
            long sizeAfterFirstUse = cache.size();
            DateTimeUtil.getDateTimeString(SAMPLE, "yyyy/MM/dd");
            assertThat(cache.size()).isEqualTo(sizeAfterFirstUse);

            for (int i = 0; i < 300; i++) {
                DateTimeUtil.getDateTimeString(SAMPLE, "yyyy-MM-dd HH:mm:ss:" + i);
            }
            assertThat(cache.size()).isLessThanOrEqualTo(256);
        }
    }

    @Nested
    @DisplayName("转换")
    class Convert {

        @Test
        @DisplayName("时间戳与 LocalDateTime 互转，支持显式时区")
        void timestamps() {
            assertThat(DateTimeUtil.timestampToLocalDateTime(0L, ZoneOffset.UTC))
                    .isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            assertThat(DateTimeUtil.localDateTimeToTimestamp(LocalDateTime.of(1970, 1, 1, 0, 0, 0), ZoneOffset.UTC))
                    .isZero();

            long epoch = DateTimeUtil.localDateTimeToTimestamp(SAMPLE, ZoneId.of("Asia/Shanghai"));
            assertThat(DateTimeUtil.timestampToLocalDateTime(epoch, ZoneId.of("Asia/Shanghai"))).isEqualTo(SAMPLE);

            // 默认时区重载与显式系统时区结果一致
            assertThat(DateTimeUtil.timestampToLocalDateTime(epoch))
                    .isEqualTo(DateTimeUtil.timestampToLocalDateTime(epoch, ZoneId.systemDefault()));
            assertThat(DateTimeUtil.localDateTimeToTimestamp(SAMPLE))
                    .isEqualTo(DateTimeUtil.localDateTimeToTimestamp(SAMPLE, ZoneId.systemDefault()));
        }

        @Test
        @DisplayName("Date 与 LocalDateTime/LocalDate 互转")
        void dates() {
            Date date = DateTimeUtil.localDateTimeToDate(SAMPLE);
            assertThat(DateTimeUtil.dateToLocalDateTime(date)).isEqualTo(SAMPLE);
            assertThat(DateTimeUtil.dateToLocalDate(date)).isEqualTo(SAMPLE.toLocalDate());
            assertThat(DateTimeUtil.localDateTimeToDate(DateTimeUtil.dateToLocalDateTime(date))).isEqualTo(date);
        }

        @Test
        @DisplayName("转换入参为 null 或时区为 null 时抛异常")
        void conversionsRejectNull() {
            assertThatThrownBy(() -> DateTimeUtil.timestampToLocalDateTime(0L, null))
                    .isInstanceOf(BizException.class).hasMessage("时区不能为空");
            assertThatThrownBy(() -> DateTimeUtil.localDateTimeToTimestamp(null))
                    .isInstanceOf(BizException.class).hasMessage("时间不能为空");
            assertThatThrownBy(() -> DateTimeUtil.dateToLocalDateTime(null))
                    .isInstanceOf(BizException.class).hasMessage("时间不能为空");
            assertThatThrownBy(() -> DateTimeUtil.dateToLocalDate(null))
                    .isInstanceOf(BizException.class).hasMessage("日期不能为空");
        }
    }

    @Nested
    @DisplayName("区间与边界")
    class Range {

        @Test
        @DisplayName("isBetween 含边界")
        void isBetween() {
            LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 9, 30, 23, 59, 59);

            assertThat(DateTimeUtil.isBetween(SAMPLE, start, end)).isTrue();
            assertThat(DateTimeUtil.isBetween(start, start, end)).isTrue();
            assertThat(DateTimeUtil.isBetween(end, start, end)).isTrue();
            assertThat(DateTimeUtil.isBetween(start.minusNanos(1), start, end)).isFalse();
            assertThat(DateTimeUtil.isBetween(end.plusNanos(1), start, end)).isFalse();

            LocalDate date = LocalDate.of(2026, 9, 4);
            assertThat(DateTimeUtil.isBetween(date, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).isTrue();
            assertThat(DateTimeUtil.isBetween(date, date, date)).isTrue();
            assertThat(DateTimeUtil.isBetween(LocalDate.of(2026, 8, 31), date, date)).isFalse();
        }

        @Test
        @DisplayName("区间判断的 null 校验")
        void isBetweenRejectsNull() {
            assertThatThrownBy(() -> DateTimeUtil.isBetween(null, LocalDateTime.now(), LocalDateTime.now()))
                    .isInstanceOf(BizException.class).hasMessage("时间不能为空");
            assertThatThrownBy(() -> DateTimeUtil.isBetween(LocalDate.now(), null, LocalDate.now()))
                    .isInstanceOf(BizException.class).hasMessage("开始日期不能为空");
            assertThatThrownBy(() -> DateTimeUtil.isBetween(LocalDate.now(), LocalDate.now(), null))
                    .isInstanceOf(BizException.class).hasMessage("结束日期不能为空");
        }

        @Test
        @DisplayName("差值计算支持负数结果")
        void differences() {
            LocalDateTime earlier = LocalDateTime.of(2024, 1, 1, 0, 0, 0);

            assertThat(DateTimeUtil.betweenYears(earlier, SAMPLE)).isEqualTo(2);
            assertThat(DateTimeUtil.betweenMonths(earlier, SAMPLE)).isEqualTo(32);
            assertThat(DateTimeUtil.betweenDays(earlier, SAMPLE)).isPositive();
            // 各单位独立截断，换算时同样按整除比较
            assertThat(DateTimeUtil.betweenMinutes(earlier, SAMPLE) / 60)
                    .isEqualTo(DateTimeUtil.betweenHours(earlier, SAMPLE));
            assertThat(DateTimeUtil.betweenSeconds(earlier, SAMPLE) / 60)
                    .isEqualTo(DateTimeUtil.betweenMinutes(earlier, SAMPLE));

            assertThat(DateTimeUtil.betweenDays(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 1))).isEqualTo(-3);
            assertThat(DateTimeUtil.betweenDays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))).isEqualTo(59);
        }

        @Test
        @DisplayName("起点/终点/星期，null 时取当前时间")
        void dayBoundaries() {
            LocalDate date = LocalDate.of(2026, 9, 4);

            assertThat(DateTimeUtil.getStartOfDay(date)).isEqualTo(LocalDateTime.of(2026, 9, 4, 0, 0, 0));
            assertThat(DateTimeUtil.getEndOfDay(date)).isEqualTo(LocalDateTime.of(2026, 9, 4, 23, 59, 59, 999_999_999));
            assertThat(DateTimeUtil.getDayOfWeek(LocalDateTime.of(2026, 9, 4, 0, 0))).isEqualTo(DayOfWeek.FRIDAY);

            assertThat(DateTimeUtil.getStartOfDay(null)).isEqualTo(LocalDate.now().atStartOfDay());
            assertThat(DateTimeUtil.getEndOfDay(null)).isEqualTo(LocalDate.now().atTime(LocalTime.MAX));
            assertThat(DateTimeUtil.getDayOfWeek(null)).isEqualTo(LocalDateTime.now().getDayOfWeek());
        }
    }
}
