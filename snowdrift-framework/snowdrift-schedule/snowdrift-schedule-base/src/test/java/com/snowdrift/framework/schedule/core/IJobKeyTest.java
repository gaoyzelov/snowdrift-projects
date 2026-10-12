package com.snowdrift.framework.schedule.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IJobKeyTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 任务标识抽象黑盒测试：getValue 以 Object 承载 Quartz（String）与 XXL-JOB（Long）
 * 两种定位差异，允许 null 值，调用方需自行判空
 * @since 1.0.0
 */
class IJobKeyTest {

    @Test
    @DisplayName("getValue 承载任意类型：Quartz 用字符串、XXL-JOB 用数字 ID")
    void exposesArbitraryValueTypes() {
        IJobKey quartzKey = () -> "jobName::jobGroup";
        IJobKey xxlJobKey = () -> 123L;

        assertThat(quartzKey.getValue()).isInstanceOf(String.class).isEqualTo("jobName::jobGroup");
        assertThat(xxlJobKey.getValue()).isInstanceOf(Long.class).isEqualTo(123L);
    }

    @Test
    @DisplayName("边界：getValue 允许返回 null，接口不做非空约束")
    void nullValueTolerated() {
        IJobKey nullKey = () -> null;

        assertThat(nullKey.getValue()).isNull();
    }
}
