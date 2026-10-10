package com.snowdrift.framework.mq.rocketmq.support;

import com.snowdrift.framework.mq.exception.MqException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RocketDelayLevelsTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RocketMQ 延迟级别映射单元测试：级别边界、就近向上映射、超上限钳制与非法入参
 * @since 1.0.0
 */
class RocketDelayLevelsTest {

    @Test
    @DisplayName("精确命中各级别边界秒数")
    void exactLevelBoundaries() {
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(1))).isEqualTo(1);
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(5))).isEqualTo(2);
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(10))).isEqualTo(3);
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(30))).isEqualTo(4);
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(60))).isEqualTo(5);
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(7200))).isEqualTo(18);
    }

    @Test
    @DisplayName("非边界值就近向上映射到不小于目标时长的最小级别")
    void mapsUpToNearestLevel() {
        // 2s 落在 (1,5] → 级别 2
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(2))).isEqualTo(2);
        // 6s 落在 (5,10] → 级别 3
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(6))).isEqualTo(3);
        // 31s 落在 (30,60] → 级别 5
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(31))).isEqualTo(5);
    }

    @Test
    @DisplayName("亚秒延迟映射到最小级别 1")
    void subSecondMapsToLevelOne() {
        assertThat(RocketDelayLevels.map(Duration.ofMillis(500))).isEqualTo(1);
    }

    @Test
    @DisplayName("超过最大级别（2h）钳制到 18")
    void clampsToMaxLevel() {
        assertThat(RocketDelayLevels.map(Duration.ofSeconds(7201))).isEqualTo(18);
        assertThat(RocketDelayLevels.map(Duration.ofDays(1))).isEqualTo(18);
    }

    @Test
    @DisplayName("非法时长（null/0/负）抛 MqException")
    void rejectsNonPositiveDuration() {
        assertThatThrownBy(() -> RocketDelayLevels.map(null)).isInstanceOf(MqException.class);
        assertThatThrownBy(() -> RocketDelayLevels.map(Duration.ZERO)).isInstanceOf(MqException.class);
        assertThatThrownBy(() -> RocketDelayLevels.map(Duration.ofSeconds(-1))).isInstanceOf(MqException.class);
    }
}
