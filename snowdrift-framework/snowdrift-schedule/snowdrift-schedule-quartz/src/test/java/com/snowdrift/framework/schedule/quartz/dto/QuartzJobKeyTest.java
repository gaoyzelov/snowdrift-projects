package com.snowdrift.framework.schedule.quartz.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QuartzJobKeyTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Quartz 任务标识黑盒测试：工厂构造、getValue 映射为原生 JobKey、值相等语义
 * @since 1.0.0
 */
class QuartzJobKeyTest {

    @Test
    @DisplayName("newInstance 填充 name/group")
    void newInstance() {
        QuartzJobKey key = QuartzJobKey.newInstance("cleanJob", "maintenance");

        assertThat(key.getName()).isEqualTo("cleanJob");
        assertThat(key.getGroup()).isEqualTo("maintenance");
    }

    @Test
    @DisplayName("getValue 返回等值的原生 org.quartz.JobKey")
    void getValueMapsToQuartzJobKey() {
        QuartzJobKey key = QuartzJobKey.newInstance("cleanJob", "maintenance");

        Object value = key.getValue();

        assertThat(value).isInstanceOf(JobKey.class);
        assertThat(value).isEqualTo(JobKey.jobKey("cleanJob", "maintenance"));
    }

    @Test
    @DisplayName("同 name/group 的两个 key 相等且 hashCode 一致（@Data 值语义）")
    void valueEquality() {
        QuartzJobKey a = QuartzJobKey.newInstance("j", "g");
        QuartzJobKey b = QuartzJobKey.newInstance("j", "g");
        QuartzJobKey c = QuartzJobKey.newInstance("j", "other");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
