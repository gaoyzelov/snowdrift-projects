package com.snowdrift.framework.schedule.xxljob.enums;

import com.snowdrift.framework.base.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BlockStrategyEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 阻塞策略枚举黑盒测试：锁定 code（XXL-JOB 线路协议值，对应 ExecutorBlockStrategyEnum）与反查
 * @since 1.0.0
 */
class BlockStrategyEnumTest {

    @Test
    @DisplayName("code 与枚举名一致（线路协议契约），共 3 种策略")
    void codeContract() {
        assertThat(BlockStrategyEnum.values()).hasSize(3);
        for (BlockStrategyEnum e : BlockStrategyEnum.values()) {
            assertThat(e.getCode()).isEqualTo(e.name());
        }
        assertThat(BlockStrategyEnum.SERIAL_EXECUTION.getNote()).isEqualTo("单机串行");
        assertThat(BlockStrategyEnum.DISCARD_LATER.getNote()).isEqualTo("丢弃后续调度");
        assertThat(BlockStrategyEnum.COVER_EARLY.getNote()).isEqualTo("覆盖之前调度");
    }

    @Test
    @DisplayName("IEnum.getByCode：命中与未知值")
    void getByCode() {
        assertThat(IEnum.getByCode(BlockStrategyEnum.class, "COVER_EARLY")).contains(BlockStrategyEnum.COVER_EARLY);
        assertThat(IEnum.getByCode(BlockStrategyEnum.class, "NOPE")).isEmpty();
    }
}
