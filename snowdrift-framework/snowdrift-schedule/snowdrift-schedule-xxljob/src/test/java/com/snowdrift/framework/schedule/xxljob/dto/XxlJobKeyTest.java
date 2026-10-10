package com.snowdrift.framework.schedule.xxljob.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XxlJobKeyTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XXL-JOB 任务标识黑盒测试：工厂构造、getValue 返回数字 id、值相等语义
 * @since 1.0.0
 */
class XxlJobKeyTest {

    @Test
    @DisplayName("newInstance 填充 id/groupId")
    void newInstance() {
        XxlJobKey key = XxlJobKey.newInstance(42, 7);

        assertThat(key.getId()).isEqualTo(42);
        assertThat(key.getGroupId()).isEqualTo(7);
    }

    @Test
    @DisplayName("getValue 返回数字 id（XXL-JOB 以 id 定位任务）")
    void getValueReturnsId() {
        assertThat(XxlJobKey.newInstance(42, 7).getValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("同 id/groupId 相等且 hashCode 一致；不同则不等")
    void valueEquality() {
        XxlJobKey a = XxlJobKey.newInstance(42, 7);
        XxlJobKey b = XxlJobKey.newInstance(42, 7);
        XxlJobKey c = XxlJobKey.newInstance(43, 7);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
