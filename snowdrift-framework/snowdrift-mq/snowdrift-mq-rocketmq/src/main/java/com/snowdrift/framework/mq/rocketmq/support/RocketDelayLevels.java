package com.snowdrift.framework.mq.rocketmq.support;

import com.snowdrift.framework.mq.exception.MqException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * RocketMQ 延迟级别映射（纯函数，便于单测）
 * <p>RocketMQ 原生延迟级别 1~18 固定；按“不超过目标时长的最小级别”就近映射，超过 2h 钳制到级别 18。</p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public final class RocketDelayLevels {

    /** RocketMQ 延迟级别从 1 开始，索引 0 为占位值 */
    private static final long[] LEVEL_SECONDS = {
            0, 1, 5, 10, 30, 60, 120, 180, 240, 300,
            360, 420, 480, 540, 600, 1200, 1800, 3600, 7200
    };

    private RocketDelayLevels() {
    }

    /**
     * 把延迟时长映射为 RocketMQ 延迟级别（1~18）
     *
     * @param delay 延迟时长，须为正
     * @return 延迟级别
     * @throws MqException delay 为 null / 非正
     */
    public static int map(Duration delay) {
        if (delay == null || delay.isNegative() || delay.isZero()) {
            throw new MqException("延迟时长必须大于 0");
        }
        long seconds = delay.getSeconds();
        for (int i = 1; i < LEVEL_SECONDS.length; i++) {
            if (seconds <= LEVEL_SECONDS[i]) {
                return i;
            }
        }
        log.warn("延迟时长 {} 超过 RocketMQ 最大延迟级别（2h），将使用级别 18", delay);
        return 18;
    }
}
