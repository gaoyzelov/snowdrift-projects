package com.snowdrift.framework.schedule.enums;

import com.snowdrift.framework.base.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MisfireStrategyEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 调度过期策略枚举黑盒测试：锁定 code（线路协议值，Quartz/XXL-JOB 共用）与 IEnum 反查
 * @since 1.0.0
 */
class MisfireStrategyEnumTest {

    @Test
    @DisplayName("code/note 契约稳定（code 直接作为 XXL-JOB misfireStrategy 线路值）")
    void codeAndNoteContract() {
        assertThat(MisfireStrategyEnum.DO_NOTHING.getCode()).isEqualTo("DO_NOTHING");
        assertThat(MisfireStrategyEnum.FIRE_ONCE_NOW.getCode()).isEqualTo("FIRE_ONCE_NOW");
        assertThat(MisfireStrategyEnum.DO_NOTHING.getNote()).isEqualTo("忽略");
        assertThat(MisfireStrategyEnum.FIRE_ONCE_NOW.getNote()).isEqualTo("立即执行一次");
    }

    @Test
    @DisplayName("恰好 2 种策略")
    void twoStrategies() {
        assertThat(MisfireStrategyEnum.values()).hasSize(2);
    }

    @Test
    @DisplayName("IEnum.getByCode：命中与未知 code 返回空 Optional")
    void getByCode() {
        assertThat(IEnum.getByCode(MisfireStrategyEnum.class, "DO_NOTHING"))
                .contains(MisfireStrategyEnum.DO_NOTHING);
        assertThat(IEnum.getByCode(MisfireStrategyEnum.class, "FIRE_ONCE_NOW"))
                .contains(MisfireStrategyEnum.FIRE_ONCE_NOW);
        assertThat(IEnum.getByCode(MisfireStrategyEnum.class, "UNKNOWN")).isEmpty();
    }
}
