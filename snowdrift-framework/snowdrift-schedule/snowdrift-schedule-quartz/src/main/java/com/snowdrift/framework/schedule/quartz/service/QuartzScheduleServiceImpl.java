package com.snowdrift.framework.schedule.quartz.service;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.DateTimeUtil;
import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.dto.JobDetails;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import com.snowdrift.framework.schedule.quartz.config.QuartzProperties;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobKey;
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
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Slf4j
public class QuartzScheduleServiceImpl implements IScheduleService<QuartzJobRequest, QuartzJobKey> {

    private final Scheduler scheduler;
    // 预留配置属性
    private final QuartzProperties quartzProperties;

    public QuartzScheduleServiceImpl(Scheduler scheduler, QuartzProperties quartzProperties) {
        this.scheduler = scheduler;
        this.quartzProperties = quartzProperties;
    }

    // ========== 任务管理 ==========

    @Override
    public QuartzJobKey addJob(QuartzJobRequest request) {
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
            return QuartzJobKey.newInstance(request.getName(),request.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务注册失败: name={}, group={}", request.getName(), request.getGroup(), e);
            throw new BizException("schedule.job.register.failed",
                    new Object[]{request.getName(), e.getMessage()});
        }
    }

    @Override
    public void removeJob(QuartzJobKey jobKey) {
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
    public void updateJob(QuartzJobKey jobKey, QuartzJobRequest request) {
        if (request.getJobClass() == null) {
            throw new BizException("schedule.job.update.failed",
                    new Object[]{jobKey.getName(), "jobClass 不能为空"});
        }
        JobKey qJobKey = JobKey.jobKey(jobKey.getName(), jobKey.getGroup());
        TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
        try {
            if (!scheduler.checkExists(qJobKey)) {
                throw new BizException("schedule.job.update.failed",
                        new Object[]{jobKey.getName(), "任务不存在"});
            }

            // 原地替换 JobDetail（参数、描述等）
            JobDetail detail = JobBuilder.newJob(request.getJobClass())
                    .withIdentity(qJobKey)
                    .withDescription(request.getDescription())
                    .usingJobData(new JobDataMap(request.getParams()))
                    .storeDurably()
                    .build();
            scheduler.addJob(detail, true);

            // 重新构建 CronTrigger 并 reschedule
            CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(request.getCron());
            if (MisfireStrategyEnum.FIRE_ONCE_NOW == request.getMisfireStrategy()) {
                cronBuilder = cronBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else {
                cronBuilder = cronBuilder.withMisfireHandlingInstructionDoNothing();
            }
            CronTrigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(cronBuilder)
                    .build();
            scheduler.rescheduleJob(triggerKey, newTrigger);

            log.info("Quartz 任务更新成功: name={}, group={}, cron={}",
                    request.getName(), request.getGroup(), request.getCron());
        } catch (SchedulerException e) {
            log.error("Quartz 任务更新失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new BizException("schedule.job.update.failed",
                    new Object[]{jobKey.getName(), e.getMessage()});
        }
    }

    @Override
    public void pauseJob(QuartzJobKey jobKey) {
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
    public void resumeJob(QuartzJobKey jobKey) {
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
    public void triggerJob(QuartzJobKey jobKey, Map<String, Object> params) {
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
    public boolean exists(QuartzJobKey jobKey) {
        try {
            return scheduler.checkExists(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
        } catch (SchedulerException e) {
            log.error("Quartz 任务查询失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new BizException("schedule.job.query.failed",
                    new Object[]{jobKey.getName(), e.getMessage()});
        }
    }

    @Override
    public JobDetails getJob(QuartzJobKey jobKey) {
        try {
            JobDetail detail = scheduler.getJobDetail(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
            if (detail == null) {
                return null;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (!(trigger instanceof CronTrigger cronTrigger)) {
                log.warn("Quartz 任务非 CronTrigger，暂不支持: name={}, group={}", jobKey.getName(), jobKey.getGroup());
                return null;
            }

            JobDetails info = new JobDetails();
            info.setJobKey(QuartzJobKey.newInstance(jobKey.getName(), jobKey.getGroup()));
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
                JobDetails info = getJob(QuartzJobKey.newInstance(jobKey.getName(), jobKey.getGroup()));
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
     */
    private JobStatusEnum toJobStatus(Trigger.TriggerState state) {
        return switch (state) {
            case NORMAL   -> JobStatusEnum.NORMAL;
            case PAUSED   -> JobStatusEnum.PAUSED;
            case COMPLETE -> JobStatusEnum.COMPLETE;
            case ERROR    -> JobStatusEnum.ERROR;
            case BLOCKED  -> JobStatusEnum.BLOCKED;
            case NONE     -> JobStatusEnum.NONE;
        };
    }
}
