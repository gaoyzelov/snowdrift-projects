package com.snowdrift.framework.schedule.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MisfireStrategyEnum
 *
 * @author gaoyzelov
 * @date 2026/5/19-17:19
 * @description 调度过期策略
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MisfireStrategyEnum implements IEnum<String> {

    DO_NOTHING("DO_NOTHING","忽略"),
    FIRE_ONCE_NOW("FIRE_ONCE_NOW","立即执行一次");

    private final String code;
    private final String note;
}