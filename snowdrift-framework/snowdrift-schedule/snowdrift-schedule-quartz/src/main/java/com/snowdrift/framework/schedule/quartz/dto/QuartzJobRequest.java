package com.snowdrift.framework.schedule.quartz.dto;

import com.snowdrift.framework.schedule.dto.JobRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.quartz.Job;

/**
 * Quartz 任务请求 — 继承公共字段，扩展 Quartz 独有配置
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class QuartzJobRequest extends JobRequest {

    /**
     * 任务类
     * — Quartz Job class
     */
    private Class<? extends Job> jobClass;
}
