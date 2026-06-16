package com.snowdrift.framework.schedule.dto;

import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务详情 VO — 查询时返回的任务完整信息
 * <p>
 * 包含任务配置元数据 + 运行时状态（仅查询时填充），屏蔽 Quartz / XXL-JOB 差异。
 * </p>
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
public class JobDetails implements Serializable {

    /** 任务名称 — Quartz JobKey.name / XXL-JOB executorHandler */
    private String name;

    /** 任务分组 — Quartz JobKey.group / XXL-JOB jobGroup */
    private String group;

    /** Cron 表达式 — Quartz CronTrigger / XXL-JOB scheduleConf */
    private String cron;

    /** 任务描述 */
    private String description;

    /** 任务参数 — Quartz JobDataMap / XXL-JOB executorParam（JSON 反序列化） */
    private Map<String, Object> params;

    /** 运行时状态 */
    private JobStatusEnum status;

    /** 上次执行时间 */
    private LocalDateTime lastFireTime;

    /** 下次执行时间 */
    private LocalDateTime nextFireTime;
}
