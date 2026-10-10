package com.snowdrift.framework.schedule.xxljob.enums;

import com.snowdrift.framework.base.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RouteStrategyEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 路由策略枚举黑盒测试：锁定 code（XXL-JOB 线路协议值，对应 ExecutorRouteStrategyEnum）与反查
 * @since 1.0.0
 */
class RouteStrategyEnumTest {

    @Test
    @DisplayName("code 与枚举名一致（线路协议契约），共 10 种策略")
    void codeContract() {
        assertThat(RouteStrategyEnum.values()).hasSize(10);
        for (RouteStrategyEnum e : RouteStrategyEnum.values()) {
            assertThat(e.getCode()).isEqualTo(e.name());
        }
        assertThat(RouteStrategyEnum.RANDOM.getCode()).isEqualTo("RANDOM");
        assertThat(RouteStrategyEnum.FAILOVER.getCode()).isEqualTo("FAILOVER");
        assertThat(RouteStrategyEnum.SHARDING_BROADCAST.getNote()).isEqualTo("分片广播");
    }

    @Test
    @DisplayName("IEnum.getByCode：命中与未知值")
    void getByCode() {
        assertThat(IEnum.getByCode(RouteStrategyEnum.class, "FIRST")).contains(RouteStrategyEnum.FIRST);
        assertThat(IEnum.getByCode(RouteStrategyEnum.class, "NOPE")).isEmpty();
    }
}
