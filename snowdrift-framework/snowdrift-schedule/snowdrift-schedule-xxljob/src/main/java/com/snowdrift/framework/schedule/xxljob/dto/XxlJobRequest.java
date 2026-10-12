package com.snowdrift.framework.schedule.xxljob.dto;

import com.snowdrift.framework.schedule.model.JobRequest;
import com.snowdrift.framework.schedule.xxljob.enums.BlockStrategyEnum;
import com.snowdrift.framework.schedule.xxljob.enums.RouteStrategyEnum;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * XXL-JOB 任务请求 — 继承公共字段，扩展 XXL-JOB 独有配置
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class XxlJobRequest extends JobRequest {

    /**
     * 作者
     */
    private String author;

    /**
     * 报警邮件
     */
    private String alarmEmail;

    /**
     * 路由策略
     */
    private RouteStrategyEnum routeStrategy = RouteStrategyEnum.RANDOM;

    /**
     * 阻塞策略
     */
    private BlockStrategyEnum blockStrategy = BlockStrategyEnum.SERIAL_EXECUTION;

    /**
     * 超时时间,单位：秒，默认0，大于0时生效
     */
    private Integer timeout = 0;

    /**
     * 重试次数，默认0，大于0时生效
     */
    private Integer retryCount = 0;

    /**
     * 任务运行状态：0=停止，1=运行
     * <p>
     * <b>{@code null} 会被当作 1（运行）提交</b>，对 {@code updateJob} 而言这意味着一件事：
     * 只改 cron、没带该字段的调用，会把一个<b>已暂停的任务重新启动</b>。要「保持当前状态」，
     * 必须显式回填：{@code request.setTriggerStatus(getJob(jobKey).getStatus() == JobStatusEnum.PAUSED ? 0 : 1)}，
     * 或先调 {@code getJob} 查询后再更新。
     * </p>
     * <p>
     * 之所以不做「{@code null} 时自动回查当前状态」：xxl-job admin 没有实现本地状态缓存，
     * 得知旧状态只能额外发一次 HTTP 请求（更新由此从 1 次请求变成 2 次）。这是一处
     * <b>有意的取舍</b>——由调用方按需选择是否付出这次回查。因此 update 成功日志会打印
     * 实际提交的 {@code triggerStatus}，便于在日志里确认任务没有被意外重启。
     * </p>
     */
    private Integer triggerStatus;
}
