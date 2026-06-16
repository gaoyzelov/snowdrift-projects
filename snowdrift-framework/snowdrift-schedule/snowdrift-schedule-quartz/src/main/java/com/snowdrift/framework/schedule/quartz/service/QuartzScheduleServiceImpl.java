package com.snowdrift.framework.schedule.quartz.service;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.DateTimeUtil;
import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.dto.JobDetails;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import com.snowdrift.framework.schedule.quartz.dto.QuartzIJobKey;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobRequest;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quartz 调度服务实现
 * <p>
 * 通过本地 {@link Scheduler} API 实现任务的动态管理。
 * 任务执行类由调用方通过 {@link QuartzJobRequest#getJobClass()} 指定。
 * </p>
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Slf4j
public class QuartzScheduleServiceImpl implements IScheduleService<QuartzJobRequest, QuartzIJobKey> {

    private final Scheduler scheduler;

    public QuartzScheduleServiceImpl(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    // ========== 任务管理 ==========

    @Override
    public void addJob(QuartzJobRequest request) {
        if (request.getJobClass() == null) {
            throw new BizException("schedule.job.register.failed",
                    new Object[]{request.getName(), "jobClass 不能为空"});
        }
        JobKey jobKey = JobKey.jobKey(request.getName(), request.getGroup());
        try {
            if (scheduler.checkExists(jobKey)) {
                throw new BizException("schedule.job.already.exists", new Object[]{request.getName()});
            }

            JobDetail detail = JobBuilder.newJob(request.getJobClass())
                    .withIdentity(jobKey)
                    .withDescription(request.getDescription())
                    .usingJobData(new JobDataMap(request.getParams()))
                    .build();

            CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(request.getCron());
            if (MisfireStrategyEnum.FIRE_ONCE_NOW == request.getMisfireStrategy()) {
                cronBuilder = cronBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else {
                cronBuilder = cronBuilder.withMisfireHandlingInstructionDoNothing();
            }

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(request.getName(), request.getGroup())
                    .withSchedule(cronBuilder)
                    .build();

            scheduler.scheduleJob(detail, trigger);
            log.info("Quartz 任务注册成功: name={}, group={}, cron={}",
                    request.getName(), request.getGroup(), request.getCron());
        } catch (SchedulerException e) {
            log.error("Quartz 任务注册失败: name={}, group={}", request.getName(), request.getGroup(), e);
            throw new BizException("schedule.job.register.failed",
                    new Object[]{request.getName(), e.getMessage()});
        }
    }

    @Override
    public void removeJob(QuartzIJobKey jobKey) {
        try {
            scheduler.deleteJob(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
            log.info("Quartz 任务删除成功: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务删除失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new BizException("schedule.job.remove.failed",
                    new Object[]{jobKey.getName(), e.getMessage()});
        }
    }

    @Override
    public void pauseJob(QuartzIJobKey jobKey) {
        try {
            scheduler.pauseJob(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
            log.info("Quartz 任务暂停: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务暂停失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new BizException("schedule.job.pause.failed",
                    new Object[]{jobKey.getName(), e.getMessage()});
        }
    }

    @Override
    public void resumeJob(QuartzIJobKey jobKey) {
        try {
            scheduler.resumeJob(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
            log.info("Quartz 任务恢复: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务恢复失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new BizException("schedule.job.resume.failed",
                    new Object[]{jobKey.getName(), e.getMessage()});
        }
    }

    @Override
    public void triggerJob(QuartzIJobKey jobKey, Map<String, Object> params) {
        try {
            JobDataMap dataMap = new JobDataMap(params);
            scheduler.triggerJob(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()), dataMap);
            log.info("Quartz 任务手动触发: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务触发失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new BizException("schedule.job.trigger.failed",
                    new Object[]{jobKey.getName(), e.getMessage()});
        }
    }

    // ========== 查询 ==========

    @Override
    public boolean exists(QuartzIJobKey jobKey) {
        try {
            return scheduler.checkExists(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
        } catch (SchedulerException e) {
            log.error("Quartz 任务查询失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            return false;
        }
    }

    @Override
    public JobDetails getJob(QuartzIJobKey jobKey) {
        try {
            JobDetail detail = scheduler.getJobDetail(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
            if (detail == null) {
                return null;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (!(trigger instanceof CronTrigger cronTrigger)) {
                return null;
            }

            JobDetails info = new JobDetails();
            info.setName(jobKey.getName());
            info.setGroup(jobKey.getGroup());
            info.setCron(cronTrigger.getCronExpression());
            info.setDescription(detail.getDescription());
            info.setParams(detail.getJobDataMap());
            info.setStatus(toJobStatus(scheduler.getTriggerState(triggerKey)));
            info.setLastFireTime(DateTimeUtil.dateToLocalDateTime(trigger.getPreviousFireTime()));
            info.setNextFireTime(DateTimeUtil.dateToLocalDateTime(trigger.getNextFireTime()));
            return info;
        } catch (SchedulerException e) {
            log.error("Quartz 任务详情查询失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            return null;
        }
    }

    @Override
    public List<JobDetails> listJobs() {
        List<JobDetails> result = new ArrayList<>();
        try {
            for (String group : scheduler.getJobGroupNames()) {
                result.addAll(listJobs(group));
            }
        } catch (SchedulerException e) {
            log.error("Quartz 任务列表查询失败", e);
        }
        return result;
    }

    @Override
    public List<JobDetails> listJobs(String group) {
        List<JobDetails> result = new ArrayList<>();
        try {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))) {
                JobDetails info = getJob(QuartzIJobKey.newInstance(jobKey.getName(), jobKey.getGroup()));
                if (info != null) {
                    result.add(info);
                }
            }
        } catch (SchedulerException e) {
            log.error("Quartz 分组任务列表查询失败: group={}", group, e);
        }
        return result;
    }

    // ========== 内部方法 ==========

    /**
     * 将 Quartz TriggerState 转换为通用 JobStatusEnum
     * <p>
     * NORMAL → NORMAL，其余状态暂时统一归为 ERROR。
     * 后续 JobStatusEnum 扩展 PAUSED 等值后同步细化映射。
     * </p>
     */
    private JobStatusEnum toJobStatus(Trigger.TriggerState state) {
        return switch (state) {
            case NORMAL -> JobStatusEnum.NORMAL;
            case PAUSED, COMPLETE, ERROR, BLOCKED, NONE -> JobStatusEnum.ERROR;
        };
    }
}
