package com.snowdrift.framework.schedule.quartz.service;

import com.snowdrift.framework.base.util.DateTimeUtil;
import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import com.snowdrift.framework.schedule.exception.ScheduleException;
import com.snowdrift.framework.schedule.model.JobDetails;
import com.snowdrift.framework.schedule.quartz.config.QuartzProperties;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobKey;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.time.LocalDateTime;
import java.util.*;

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
        if (Objects.isNull(request.getJobClass())) {
            throw new ScheduleException("Quartz 任务类型不能为空");
        }
        if (StringUtils.isBlank(request.getName()) || StringUtils.isBlank(request.getCron())) {
            throw new ScheduleException("Quartz 任务名称与 cron 表达式不能为空");
        }
        JobKey jobKey = JobKey.jobKey(request.getName(), request.getGroup());
        try {
            if (scheduler.checkExists(jobKey)) {
                throw new ScheduleException("Quartz 任务已存在");
            }

            JobDetail detail = JobBuilder.newJob(request.getJobClass())
                    .withIdentity(jobKey)
                    .withDescription(request.getDescription())
                    .usingJobData(new JobDataMap(request.getParams() != null ? request.getParams() : new HashMap<>()))
                    .build();

            CronScheduleBuilder cronBuilder = buildCronBuilder(request);
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(request.getName(), request.getGroup())
                    .withSchedule(cronBuilder)
                    .build();

            scheduler.scheduleJob(detail, trigger);
            log.info("Quartz 任务注册成功: name={}, group={}, cron={}",
                    request.getName(), request.getGroup(), request.getCron());
            return QuartzJobKey.newInstance(request.getName(), request.getGroup());
        } catch (SchedulerException e) {
            // 并发重复注册时 Quartz 抛 ObjectAlreadyExistsException，报“已存在”而非笼统“注册失败”
            if (e instanceof ObjectAlreadyExistsException) {
                throw new ScheduleException("Quartz 任务已存在");
            }
            log.error("Quartz 任务注册失败: name={}, group={}", request.getName(), request.getGroup(), e);
            throw new ScheduleException("Quartz 任务注册失败");
        }
    }

    @Override
    public void removeJob(QuartzJobKey jobKey) {
        try {
            // deleteJob 返回 false 表示任务不存在，避免静默 no-op
            boolean deleted = scheduler.deleteJob(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
            if (!deleted) {
                throw new ScheduleException("Quartz 任务不存在，无法删除");
            }
            log.info("Quartz 任务删除成功: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务删除失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务删除失败");
        }
    }

    @Override
    public void updateJob(QuartzJobKey jobKey, QuartzJobRequest request) {
        if (request.getJobClass() == null) {
            throw new ScheduleException("Quartz 任务更新失败");
        }
        if (StringUtils.isBlank(request.getName()) || StringUtils.isBlank(request.getCron())) {
            throw new ScheduleException("Quartz 任务名称与 cron 表达式不能为空");
        }
        JobKey qJobKey = JobKey.jobKey(jobKey.getName(), jobKey.getGroup());
        TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
        try {
            if (!scheduler.checkExists(qJobKey)) {
                throw new ScheduleException("Quartz 任务更新失败");
            }

            // 1. 保存原始 Trigger（用于回滚）；不存在则显式失败，避免 rescheduleJob 静默 no-op
            Trigger originalTrigger = scheduler.getTrigger(triggerKey);
            if (originalTrigger == null) {
                throw new ScheduleException("Quartz 任务触发器不存在，无法更新");
            }

            // 2. 先更新 Trigger（可逆操作在前）
            CronScheduleBuilder cronBuilder = buildCronBuilder(request);
            CronTrigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(cronBuilder)
                    .build();
            scheduler.rescheduleJob(triggerKey, newTrigger);

            // 3. 再更新 JobDetail，失败时回滚 Trigger
            JobDetail detail = JobBuilder.newJob(request.getJobClass())
                    .withIdentity(qJobKey)
                    .withDescription(request.getDescription())
                    .usingJobData(new JobDataMap(request.getParams() != null ? request.getParams() : new HashMap<>()))
                    .storeDurably()
                    .build();
            try {
                scheduler.addJob(detail, true);
            } catch (SchedulerException e) {
                // JobDetail 更新失败 → 回滚 Trigger 到原始状态
                try {
                    scheduler.rescheduleJob(triggerKey, originalTrigger);
                    log.info("Quartz 任务回滚成功，Trigger 已恢复: group={}, name={}",
                            jobKey.getGroup(), jobKey.getName());
                } catch (SchedulerException rollbackEx) {
                    log.error("Quartz 任务回滚失败！Trigger 状态异常，需人工介入: group={}, name={}",
                            jobKey.getGroup(), jobKey.getName(), rollbackEx);
                }
                throw e;
            }

            log.info("Quartz 任务更新成功: name={}, group={}, cron={}",
                    request.getName(), request.getGroup(), request.getCron());
        } catch (SchedulerException e) {
            log.error("Quartz 任务更新失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务更新失败");
        }
    }

    @Override
    public void pauseJob(QuartzJobKey jobKey) {
        try {
            JobKey qJobKey = JobKey.jobKey(jobKey.getName(), jobKey.getGroup());
            if (!scheduler.checkExists(qJobKey)) {
                throw new ScheduleException("Quartz 任务不存在，无法暂停");
            }
            scheduler.pauseJob(qJobKey);
            log.info("Quartz 任务暂停: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务暂停失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务暂停失败");
        }
    }

    @Override
    public void resumeJob(QuartzJobKey jobKey) {
        try {
            JobKey qJobKey = JobKey.jobKey(jobKey.getName(), jobKey.getGroup());
            if (!scheduler.checkExists(qJobKey)) {
                throw new ScheduleException("Quartz 任务不存在，无法恢复");
            }
            scheduler.resumeJob(qJobKey);
            log.info("Quartz 任务恢复: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务恢复失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务恢复失败");
        }
    }

    @Override
    public void triggerJob(QuartzJobKey jobKey, Map<String, Object> params) {
        try {
            JobDataMap dataMap = new JobDataMap(params != null ? params : new HashMap<>());
            scheduler.triggerJob(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()), dataMap);
            log.info("Quartz 任务手动触发: name={}, group={}", jobKey.getName(), jobKey.getGroup());
        } catch (SchedulerException e) {
            log.error("Quartz 任务触发失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务触发失败");
        }
    }

    // ========== 查询 ==========

    @Override
    public boolean exists(QuartzJobKey jobKey) {
        try {
            return scheduler.checkExists(JobKey.jobKey(jobKey.getName(), jobKey.getGroup()));
        } catch (SchedulerException e) {
            log.error("Quartz 任务查询失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务查询失败");
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

            JobDetails info = new JobDetails();
            info.setJobKey(QuartzJobKey.newInstance(jobKey.getName(), jobKey.getGroup()));
            info.setName(jobKey.getName());
            info.setGroup(jobKey.getGroup());
            // 仅 CronTrigger 才解析 cron；SimpleTrigger 等其他触发器置空，保证 list/get/exists 口径一致
            info.setCron(trigger instanceof CronTrigger cronTrigger ? cronTrigger.getCronExpression() : null);
            info.setDescription(detail.getDescription());
            // 返回副本，避免外部篡改 JobDetail 持有的 JobDataMap
            info.setParams(detail.getJobDataMap() != null ? new HashMap<>(detail.getJobDataMap()) : null);
            info.setStatus(toJobStatus(scheduler.getTriggerState(triggerKey)));
            if (trigger != null) {
                // 从未触发/已结束的任务 Previous/NextFireTime 可能为 null，需空安全
                info.setLastFireTime(toLocalDateTime(trigger.getPreviousFireTime()));
                info.setNextFireTime(toLocalDateTime(trigger.getNextFireTime()));
            }
            return info;
        } catch (SchedulerException e) {
            log.error("Quartz 任务详情查询失败: name={}, group={}", jobKey.getName(), jobKey.getGroup(), e);
            throw new ScheduleException("Quartz 任务详情查询失败");
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
            throw new ScheduleException("Quartz 任务列表查询失败");
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
            throw new ScheduleException("Quartz 分组任务列表查询失败");
        }
        return result;
    }

    // ========== 内部方法 ==========

    /**
     * 构建 CronScheduleBuilder 并应用 misfire 策略；cron 非法时统一抛 {@link ScheduleException}，避免裸 RuntimeException 外泄
     */
    private CronScheduleBuilder buildCronBuilder(QuartzJobRequest request) {
        final CronScheduleBuilder builder;
        try {
            builder = CronScheduleBuilder.cronSchedule(request.getCron());
        } catch (RuntimeException e) {
            log.error("Quartz cron 表达式非法: name={}, group={}, cron={}",
                    request.getName(), request.getGroup(), request.getCron(), e);
            throw new ScheduleException("Quartz cron 表达式非法：" + request.getCron());
        }
        // DO_NOTHING 需显式指定；null 及其他取值（含 DTO 默认 FIRE_ONCE_NOW）统一映射为立即执行一次，与 XXL-JOB 一致
        if (MisfireStrategyEnum.DO_NOTHING == request.getMisfireStrategy()) {
            return builder.withMisfireHandlingInstructionDoNothing();
        }
        return builder.withMisfireHandlingInstructionFireAndProceed();
    }

    /**
     * java.util.Date → LocalDateTime（null 安全，用于从未触发/已结束的任务）
     */
    private LocalDateTime toLocalDateTime(Date date) {
        return date != null ? DateTimeUtil.dateToLocalDateTime(date) : null;
    }

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
