package com.snowdrift.framework.schedule.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 * <p>
 * 完全对齐 Quartz {@code TriggerState} 的 6 种状态，
 * XXL-JOB 的 {@code triggerStatus} 做兼容映射：1→NORMAL，0→PAUSED。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum JobStatusEnum implements IEnum<Integer> {

    /** 正常运行中 — Quartz NORMAL */
    NORMAL(0, "正常"),
    /** 已暂停 — Quartz PAUSED */
    PAUSED(1, "暂停"),
    /** 已完成 — Quartz COMPLETE */
    COMPLETE(2, "完成"),
    /** 执行错误 — Quartz ERROR */
    ERROR(3, "错误"),
    /** 被阻塞（有并发实例正在执行）— Quartz BLOCKED */
    BLOCKED(4, "阻塞"),
    /** 触发器不存在或已删除 — Quartz NONE */
    NONE(5, "不存在");

    private final Integer code;

    private final String note;
}
