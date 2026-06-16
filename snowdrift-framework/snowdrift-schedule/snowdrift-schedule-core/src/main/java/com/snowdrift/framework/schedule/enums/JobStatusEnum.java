package com.snowdrift.framework.schedule.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 * <p>
 * 取 Quartz TriggerState 与 XXL-JOB triggerStatus 的交集。
 * 当前仅定义 NORMAL / ERROR，后续按需扩展 PAUSED 等状态。
 * </p>
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum JobStatusEnum implements IEnum<Integer> {

    ERROR(-1, "错误"),
    NORMAL(0, "正常");

    private final Integer code;

    private final String note;
}
