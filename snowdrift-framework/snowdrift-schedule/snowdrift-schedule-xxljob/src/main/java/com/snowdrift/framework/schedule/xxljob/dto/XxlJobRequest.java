package com.snowdrift.framework.schedule.xxljob.dto;

import com.snowdrift.framework.schedule.dto.JobRequest;
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
}
