package com.snowdrift.framework.schedule.xxljob.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * XXL-JOB 阻塞处理策略 — 对应 {@code ExecutorBlockStrategyEnum}
 * <p>
 * 控制任务调度到达时，执行器已有该任务实例运行时的处理方式。
 * </p>
 *
 * @author gaoye
 * @date 2025/05/19
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum BlockStrategyEnum implements IEnum<String> {

    /** 单机串行：排队等待上一次执行完成 */
    SERIAL_EXECUTION("SERIAL_EXECUTION", "单机串行"),
    /** 丢弃后续调度：新触发被丢弃，旧任务继续 */
    DISCARD_LATER("DISCARD_LATER", "丢弃后续调度"),
    /** 覆盖之前调度：终止旧任务，执行新任务 */
    COVER_EARLY("COVER_EARLY", "覆盖之前调度");

    private final String code;
    private final String note;
}
