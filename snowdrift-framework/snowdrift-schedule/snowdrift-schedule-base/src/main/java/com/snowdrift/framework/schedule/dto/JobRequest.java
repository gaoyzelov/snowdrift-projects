package com.snowdrift.framework.schedule.dto;

import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务信息 DTO
 * <p>
 * 统一描述定时任务的元数据，屏蔽 Quartz / XXL-JOB 差异。
 * 管理端通过此对象创建和管理任务。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
public class JobRequest implements Serializable {

    /**
     * 任务名称
     * — Quartz JobKey.name / XXL-JOB executorHandlerName
     */
    private String name;

    /**
     * 任务分组
     * — Quartz JobKey.group / XXL-JOB jobGroup
     */
    private String group;

    /**
     * Cron 表达式
     * — Quartz CronTrigger / XXL-JOB scheduleConf
     */
    private String cron;

    /**
     * 任务描述
     * — Quartz job description / XXL-JOB jobDesc
     */
    private String description;

    /**
     * 任务参数
     * — Quartz JobDataMap / XXL-JOB executorParam（序列化为 JSON）
     */
    private Map<String,Object> params;

    /**
     * 调度过期策略
     * — Quartz misfire instruction / XXL-JOB misfireStrategy
     */
    private MisfireStrategyEnum misfireStrategy = MisfireStrategyEnum.FIRE_ONCE_NOW;
}
