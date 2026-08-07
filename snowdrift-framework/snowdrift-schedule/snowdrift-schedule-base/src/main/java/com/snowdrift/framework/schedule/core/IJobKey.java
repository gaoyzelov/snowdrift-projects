package com.snowdrift.framework.schedule.core;

/**
 * 任务标识抽象 — 屏蔽 Quartz / XXL-JOB 的任务定位差异
 * <p>
 * Quartz 实现使用 name + group（对应 {@code JobKey}），
 * XXL-JOB 实现使用数字 id。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
public interface IJobKey {

    Object getValue();
}
