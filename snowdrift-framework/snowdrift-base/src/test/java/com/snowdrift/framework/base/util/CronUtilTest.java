package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CronUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description cron 工具类单元测试
 * @since 1.0.0
 */
class CronUtilTest {

    @Test
    @DisplayName("getCron：秒 分 时 日 月 ？ 年")
    void getCron() {
        assertThat(CronUtil.getCron(LocalDateTime.of(2026, 9, 4, 12, 0, 0)))
                .isEqualTo("00 00 12 04 09 ? 2026");
        assertThat(CronUtil.getCron(LocalDateTime.of(1970, 1, 1, 0, 0, 1)))
                .isEqualTo("01 00 00 01 01 ? 1970");
    }

    @Test
    @DisplayName("getCron：年份越界与 null 抛出业务异常")
    void getCronValidation() {
        assertThatThrownBy(() -> CronUtil.getCron(LocalDateTime.of(1969, 12, 31, 23, 59, 59)))
                .isInstanceOf(BizException.class).hasMessage("年份必须在1970到2099之间");
        assertThatThrownBy(() -> CronUtil.getCron(LocalDateTime.of(2100, 1, 1, 0, 0, 0)))
                .isInstanceOf(BizException.class).hasMessage("年份必须在1970到2099之间");
        assertThatThrownBy(() -> CronUtil.getCron(null))
                .isInstanceOf(BizException.class).hasMessage("时间不能为空");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0 0 12 ? * MON",
            "0 0 9-18 ? * MON-FRI",
            "0 */5 * * * ?",
            "0 0 0 1 * ?",
            "0 0 0 1 1 ?",
            "0 0 0 1 1 ? 2026",
            "0 0 0 1 1 ? *",
            "0 15 10 ? * SAT,SUN",
            "0 0-30 10 ? * *",
            "0 0 0 20 * ?"})
    @DisplayName("isValid：接受 Quartz 子集内的表达式")
    void isValidAccepts(String cron) {
        assertThat(CronUtil.isValid(cron)).as(cron).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "* * * *",
            "0 0 0 ? * ?",
            "0 0 0 * * *",
            "? 0 0 1 1 ?",
            "60 0 0 * * ?",
            "0 60 0 * * ?",
            "0 0 24 * * ?",
            "0 0 0 32 * ?",
            "0 0 0 0 * ?",
            "0 0 0 * 13 ?",
            "0 0 0 * * MON#2",
            "0 0 0 L * ?",
            "0 0 0 ? * 9",
            "0 0 0 * * MON",
            "0 0 */0 * * ?",
            "0 0 */120 * * ?",
            "0 0 0 1-5-6 1 ?",
            "0 0 0 1 1 ? 1969",
            "0 0 0 1 1 ? 2100",
            "0 0 0 1 1 ? 26",
            "0 0 0 1 1 ? 2026 2027",
            "abc def ghi jkl mno pq"})
    @DisplayName("isValid：拒绝越界、语法不符或超出子集的表达式")
    void isValidRejects(String cron) {
        assertThat(CronUtil.isValid(cron)).as(cron).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void isValidRejectsBlank(String cron) {
        assertThat(CronUtil.isValid(cron)).isFalse();
    }

    @Test
    @DisplayName("isValid：接受由本类生成的表达式（含所有常量）")
    void isValidAcceptsGeneratedExpressions() {
        List<String> generated = List.of(
                CronUtil.EVERY_MINUTE, CronUtil.EVERY_HOUR, CronUtil.EVERY_DAY,
                CronUtil.EVERY_WEEK, CronUtil.EVERY_MONTH, CronUtil.EVERY_YEAR,
                CronUtil.workDay(), CronUtil.atTime(12, 30, 0), CronUtil.atWorkDayTime(9, 0, 0),
                CronUtil.everyMinutes(30), CronUtil.everyHours(2),
                CronUtil.getCron(LocalDateTime.now()));

        assertThat(generated).allSatisfy(cron -> assertThat(CronUtil.isValid(cron)).as(cron).isTrue());
    }

    @Test
    @DisplayName("everyMinutes/everyHours：边界内生成合法表达式，越界抛异常")
    void intervalFactories() {
        assertThat(CronUtil.everyMinutes(1)).isEqualTo("0 */1 * * * ?");
        assertThat(CronUtil.everyMinutes(59)).isEqualTo("0 */59 * * * ?");
        assertThat(CronUtil.everyHours(1)).isEqualTo("0 0 */1 * * ?");
        assertThat(CronUtil.everyHours(23)).isEqualTo("0 0 */23 * * ?");

        assertThatThrownBy(() -> CronUtil.everyMinutes(0))
                .isInstanceOf(BizException.class).hasMessage("分钟必须在 1 到 59 之间");
        assertThatThrownBy(() -> CronUtil.everyMinutes(60))
                .isInstanceOf(BizException.class).hasMessage("分钟必须在 1 到 59 之间");
        assertThatThrownBy(() -> CronUtil.everyHours(0))
                .isInstanceOf(BizException.class).hasMessage("小时必须在 1 到 23 之间");
        assertThatThrownBy(() -> CronUtil.everyHours(24))
                .isInstanceOf(BizException.class).hasMessage("小时必须在 1 到 23 之间");
    }

    @Test
    @DisplayName("atTime/atWorkDayTime：参数越界抛异常，正常参数生成合法表达式")
    void atTimeFactories() {
        assertThat(CronUtil.atTime(0, 0, 0)).isEqualTo("0 0 0 * * ?");
        assertThat(CronUtil.atWorkDayTime(9, 30, 5)).isEqualTo("5 30 9 ? * MON-FRI");

        assertThatThrownBy(() -> CronUtil.atTime(24, 0, 0))
                .isInstanceOf(BizException.class).hasMessage("小时必须在 0 到 23 之间");
        assertThatThrownBy(() -> CronUtil.atTime(0, 60, 0))
                .isInstanceOf(BizException.class).hasMessage("分钟必须在 0 到 59 之间");
        assertThatThrownBy(() -> CronUtil.atWorkDayTime(0, 0, 60))
                .isInstanceOf(BizException.class).hasMessage("秒必须在 0 到 59 之间");
    }

    @Test
    @DisplayName("workDay：周一至周五 9-18 点整点")
    void workDay() {
        assertThat(CronUtil.workDay()).isEqualTo("0 0 9-18 ? * MON-FRI");
    }
}
