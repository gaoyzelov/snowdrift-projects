package com.snowdrift.framework.schedule.enums;

import com.snowdrift.framework.base.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobStatusEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 任务状态枚举黑盒测试：锁定 code/note 契约、IEnum 按 code/note 反查与未知值处理、6 种状态齐备
 * @since 1.0.0
 */
class JobStatusEnumTest {

    @Test
    @DisplayName("code/note 契约稳定（对齐 Quartz TriggerState 的 6 种状态）")
    void codeAndNoteContract() {
        assertThat(JobStatusEnum.NORMAL.getCode()).isEqualTo(0);
        assertThat(JobStatusEnum.PAUSED.getCode()).isEqualTo(1);
        assertThat(JobStatusEnum.COMPLETE.getCode()).isEqualTo(2);
        assertThat(JobStatusEnum.ERROR.getCode()).isEqualTo(3);
        assertThat(JobStatusEnum.BLOCKED.getCode()).isEqualTo(4);
        assertThat(JobStatusEnum.NONE.getCode()).isEqualTo(5);

        assertThat(JobStatusEnum.NORMAL.getNote()).isEqualTo("正常");
        assertThat(JobStatusEnum.PAUSED.getNote()).isEqualTo("暂停");
        assertThat(JobStatusEnum.COMPLETE.getNote()).isEqualTo("完成");
        assertThat(JobStatusEnum.ERROR.getNote()).isEqualTo("错误");
        assertThat(JobStatusEnum.BLOCKED.getNote()).isEqualTo("阻塞");
        assertThat(JobStatusEnum.NONE.getNote()).isEqualTo("不存在");
    }

    @Test
    @DisplayName("恰好 6 种状态，无重复 code")
    void sixDistinctStates() {
        assertThat(JobStatusEnum.values()).hasSize(6);
        assertThat(JobStatusEnum.values()).extracting(JobStatusEnum::getCode).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("IEnum.getByCode：已知 code 命中，未知 code 返回空 Optional")
    void getByCode() {
        assertThat(IEnum.getByCode(JobStatusEnum.class, 0)).contains(JobStatusEnum.NORMAL);
        assertThat(IEnum.getByCode(JobStatusEnum.class, 5)).contains(JobStatusEnum.NONE);
        assertThat(IEnum.getByCode(JobStatusEnum.class, 999)).isEmpty();
        assertThat(IEnum.getByCode(JobStatusEnum.class, null)).isEmpty();
    }

    @Test
    @DisplayName("IEnum.getByNote：已知 note 命中，未知 note 返回空 Optional")
    void getByNote() {
        assertThat(IEnum.getByNote(JobStatusEnum.class, "正常")).contains(JobStatusEnum.NORMAL);
        assertThat(IEnum.getByNote(JobStatusEnum.class, "不存在")).contains(JobStatusEnum.NONE);
        assertThat(IEnum.getByNote(JobStatusEnum.class, "无此状态")).isEmpty();
    }
}
